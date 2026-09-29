package com.wallee.terminallinker.core.api

import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.api.dto.SearchResponse
import com.wallee.terminallinker.core.api.dto.WalleeJson
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DtoDecodeTest {
    @Test
    fun `decodes a full terminal search page`() {
        val json = javaClass.getResource("/mock-terminals-page.json")!!.readText()
        val page = WalleeJson.decodeFromString(serializer<SearchResponse<PaymentTerminal>>(), json)
        assertEquals(100, page.data.size)
    }
}
