package io.getstream.chat.android.livedata.controller

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.getstream.chat.android.client.api.models.Pagination
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.utils.SyncStatus
import io.getstream.chat.android.livedata.BaseConnectedIntegrationTest
import io.getstream.chat.android.livedata.utils.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelControllerReadPaginateDomainTest : BaseConnectedIntegrationTest() {


    /**
     * test that a message added only to the local storage is picked up
     */
    @Test
    fun watchSetsMessagesAndChannelOffline() = runBlocking(Dispatchers.IO) {
        chatDomain.setOffline()
        // add a message to local storage
        chatDomain.repos.users.insertUser(data.user1)
        chatDomain.repos.channels.insertChannel(data.channel1)
        val message1 = data.createMessage()
        channelController.sendMessage(message1)
        // remove the livedata
        channelController =
            ChannelController(
                data.channel1.type,
                data.channel1.id,
                chatDomain.client,
                chatDomain
            )

        // run watch while we're offline
        channelController.watch()

        // the message should still show up
        val messages = channelController.messages.getOrAwaitValue()
        val channel = channelController.channel.getOrAwaitValue()

        Truth.assertThat(messages).isNotEmpty()
        Truth.assertThat(channel).isNotNull()
        Truth.assertThat(channel.config).isNotNull()
    }

    /**
     * test that a message added only to the local storage is picked up
     */
    @Test
    fun watchSetsMessagesAndChannelOnline() = runBlocking(Dispatchers.IO) {
        chatDomain.setOnline()
        // setup an online message
        val message = Message()
        message.syncStatus = SyncStatus.SYNC_NEEDED
        // write a message
        channelController.sendMessage(message)

        val messages = channelController.messages.getOrAwaitValue()
        val channel = channelController.channel.getOrAwaitValue()

        Truth.assertThat(messages.size).isGreaterThan(0)
        Truth.assertThat(messages.first().id).isEqualTo(message.id)
        Truth.assertThat(channel).isNotNull()
        Truth.assertThat(channel.config).isNotNull()
    }

    @Test
    fun recovery() = runBlocking(Dispatchers.IO) {
        // running recover should trigger channels to show up for active queries and channels
        chatDomain.connectionRecovered(true)

        // verify channel data is loaded
        val channelRepos = queryController.channels.getOrAwaitValue()
        Truth.assertThat(channelRepos.size).isGreaterThan(0)

        // verify we have messages as well
        val channelId = channelRepos.first().cid

        val messages = chatDomain.channel(channelId).messages.getOrAwaitValue()
        Truth.assertThat(messages.size).isGreaterThan(0)
    }


    @Test
    fun loadOlderMessages() = runBlocking(Dispatchers.IO) {
        val channelRepo = chatDomain.channel("messaging", "testabc")
        Truth.assertThat(channelRepo.loading.getOrAwaitValue()).isFalse()
        channelRepo.upsertMessages(listOf(data.message1, data.message2Older))
        // verify we sort correctly
        val messages = channelRepo.sortedMessages()
        Truth.assertThat(messages[0].createdAt!!.before(messages[1].createdAt)).isTrue()
        // verify we generate the right request
        val request = channelRepo.loadMoreMessagesRequest(10, Pagination.LESS_THAN)
        // message 2 is older, we should use message 2 for getting older messages
        Truth.assertThat(request.messageFilterValue).isEqualTo(data.message2Older.id)
        // verify that running the query doesn't error
        val result = channelRepo.runChannelQueryOnline(request)
        // TODO: Mock the call to query channel
    }


}