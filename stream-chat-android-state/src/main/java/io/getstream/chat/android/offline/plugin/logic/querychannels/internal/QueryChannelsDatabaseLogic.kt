package io.getstream.chat.android.offline.plugin.logic.querychannels.internal

import androidx.annotation.VisibleForTesting
import io.getstream.chat.android.client.extensions.internal.applyPagination
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.ChannelConfig
import io.getstream.chat.android.client.models.Config
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.persistance.repository.ChannelConfigRepository
import io.getstream.chat.android.client.persistance.repository.ChannelRepository
import io.getstream.chat.android.client.persistance.repository.MessageRepository
import io.getstream.chat.android.client.persistance.repository.QueryChannelsRepository
import io.getstream.chat.android.client.persistance.repository.UserRepository
import io.getstream.chat.android.client.query.QueryChannelsSpec
import io.getstream.chat.android.client.query.pagination.AnyChannelPaginationRequest
import io.getstream.chat.android.client.query.pagination.isRequestingMoreThanLastMessage
import io.getstream.logging.StreamLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

internal class QueryChannelsDatabaseLogic(
    private val queryChannelsRepository: QueryChannelsRepository,
    private val channelConfigRepository: ChannelConfigRepository,
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val scope: CoroutineScope,
    private val defaultConfig: Config,
) {

    private val logger = StreamLog.getLogger("QueryChannelsDatabaseLogic")

    private suspend fun selectChannels(
        channelIds: List<String>,
        pagination: AnyChannelPaginationRequest?,
        forceCache: Boolean = false,
    ): List<Channel> {
        // fetch the channel entities from room
        val channels = channelRepository.selectChannels(channelIds, forceCache)
        // TODO why it is not compared this way?
        //  pagination?.isRequestingMoreThanLastMessage() == true
        val messagesMap = if (pagination?.isRequestingMoreThanLastMessage() != false) {
            // with postgres this could be optimized into a single query instead of N, not sure about sqlite on android
            // sqlite has window functions: https://sqlite.org/windowfunctions.html
            // but android runs a very dated version: https://developer.android.com/reference/android/database/sqlite/package-summary
            channelIds.map { cid ->
                scope.async { cid to messageRepository.selectMessagesForChannel(cid, pagination) }
            }.awaitAll().toMap()
        } else {
            emptyMap()
        }

        return channels.onEach { channel ->
            channel.enrichChannel(messagesMap, defaultConfig)
        }
    }

    internal suspend fun storeStateForChannels(
        configs: Collection<ChannelConfig>? = null,
        users: List<User>,
        channels: Collection<Channel>,
        messages: List<Message>,
        cacheForMessages: Boolean = false,
    ) {
        configs?.let { channelConfigRepository.insertChannelConfigs(it) }
        userRepository.insertUsers(users)
        channelRepository.insertChannels(channels)
        messageRepository.insertMessages(messages, cacheForMessages)
    }

    internal suspend fun fetchChannelsFromCache(
        pagination: AnyChannelPaginationRequest,
        queryChannelsRepository: QueryChannelsRepository,
        queryChannelsSpec: QueryChannelsSpec
    ): List<Channel> {
        val query = queryChannelsRepository.selectBy(queryChannelsSpec.filter, queryChannelsSpec.querySort)
            ?: return emptyList()

        return selectChannels(query.cids.toList(), pagination).applyPagination(pagination)
    }

    @VisibleForTesting
    fun Channel.enrichChannel(messageMap: Map<String, List<Message>>, defaultConfig: Config) {
        config = channelConfigRepository.selectChannelConfig(type)?.config ?: defaultConfig
        messages = if (messageMap.containsKey(cid)) {
            val fullList = (messageMap[cid] ?: error("Messages must be in the map")) + messages
            fullList.distinctBy(Message::id)
        } else {
            messages
        }
    }
}