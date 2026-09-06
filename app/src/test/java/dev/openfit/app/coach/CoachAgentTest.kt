package dev.openfit.app.coach

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.openfit.app.data.macro.MacroDatabase
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.data.macro.MealRepository
import dev.openfit.app.llm.ChatClient
import dev.openfit.app.llm.ChatMessage
import dev.openfit.app.llm.ChatResult
import dev.openfit.app.llm.ToolCall
import dev.openfit.app.llm.ToolSpec
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoachAgentTest {

    private lateinit var macroDb: MacroDatabase
    private lateinit var mealRepo: MealRepository

    @Before
    fun setUp() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            macroDb = MacroDatabase.inMemory(context)
            mealRepo = MealRepository(macroDb)
            mealRepo.add(
                MealEntry(
                    timestamp = LocalDate.of(2026, 1, 15).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    imagePath = "",
                    dish = "Oats",
                    calories = 300.0,
                    protein = 12.0,
                    carbs = 50.0,
                    fat = 6.0,
                )
            )
        }
    }

    @After
    fun tearDown() {
        macroDb.close()
    }

    /** A chat client whose responses are scripted per call so tests can drive the agent loop. */
    private class FakeChatClient(private val responder: (Int) -> ChatResult) : ChatClient("", "", "") {
        private var callIndex = 0
        val requests = mutableListOf<List<ChatMessage>>()

        override suspend fun chat(messages: List<ChatMessage>, tools: List<ToolSpec>): ChatResult {
            requests += messages
            return responder(callIndex++)
        }
    }

    @Test
    fun `agent executes a tool and feeds the result back before answering`() = runBlocking {
        val tool = NutritionTool(mealRepo)
        val fake = FakeChatClient { index ->
            when (index) {
                0 -> ChatResult(
                    content = null,
                    toolCalls = listOf(ToolCall("call_1", "getNutrition", """{"start":"2026-01-01","end":"2026-01-31"}""")),
                    finishReason = "tool_calls",
                )
                else -> ChatResult(content = "You logged 1 meal", toolCalls = emptyList(), finishReason = "stop")
            }
        }
        val executed = mutableListOf<String>()
        val agent = CoachAgent(tools = listOf(tool), onToolRun = { executed += it })

        val reply = agent.reply(fake, "What did I eat in January?")

        assertEquals("You logged 1 meal", reply)
        assertEquals(listOf("getNutrition"), executed)
        assertTrue("tool result must be sent back", fake.requests.last().any { it.role == ChatMessage.TOOL })
    }

    @Test
    fun `agent stops when the model answers without tool calls`() = runBlocking {
        val fake = FakeChatClient { ChatResult(content = "Hi!", toolCalls = emptyList(), finishReason = "stop") }
        val agent = CoachAgent(tools = listOf(NutritionTool(mealRepo)))
        val reply = agent.reply(fake, "hello")
        assertEquals("Hi!", reply)
        assertEquals(1, fake.requests.size)
    }

    @Test
    fun `agent returns a fallback after exceeding the step limit`() = runBlocking {
        val fake = FakeChatClient {
            ChatResult(
                content = null,
                toolCalls = listOf(ToolCall("x", "getNutrition", """{"start":"2026-01-01"}""")),
                finishReason = "tool_calls",
            )
        }
        val agent = CoachAgent(tools = listOf(NutritionTool(mealRepo)))
        val reply = agent.reply(fake, "keep running")
        assertTrue(reply.contains("step limit"))
    }
}
