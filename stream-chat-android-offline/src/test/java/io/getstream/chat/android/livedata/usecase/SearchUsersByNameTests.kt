package io.getstream.chat.android.livedata.usecase

import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.randomUser
import io.getstream.chat.android.test.TestCall
import io.getstream.chat.android.test.randomInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.amshove.kluent.When
import org.amshove.kluent.any
import org.amshove.kluent.calling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SearchUsersByNameTests {
    private lateinit var chatDomainImpl: ChatDomainImpl
    private lateinit var chatClient: ChatClient
    private lateinit var sut: SearchUsersByName

    @BeforeEach
    fun setUp() {
        chatClient = mock()
        chatDomainImpl = mock {
            on(it.client) doReturn chatClient
            on(it.scope) doReturn TestCoroutineScope()
            on(it.currentUser) doReturn randomUser()
        }
        sut = SearchUsersByName(chatDomainImpl)
    }

    @Test
    fun `Given empty search string Should perform search query with default filter`() {
        When calling chatClient.queryUsers(any()) doReturn TestCall(mock())

        sut(querySearch = "", randomInt(), randomInt()).execute()

        verify(chatClient).queryUsers(
            argThat {
                filter == sut.defaultUsersQueryFilter
            }
        )
    }
}
