package com.btello.quoridor

import com.btello.quoridor.presentation.navigationbar.Tabs
import com.btello.quoridor.presentation.navigationbar.Tabs.GAME
import com.btello.quoridor.presentation.navigationbar.Tabs.RULES
import com.btello.quoridor.presentation.navigationbar.Tabs.SETTINGS
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeTabTest {

    @Test
    fun exposesGameRulesAndSettingsInOrder() {
        assertEquals(listOf(GAME, RULES, SETTINGS), Tabs.entries)
    }

    @Test
    fun everyTabHasDistinctLabelAndIconResources() {
        val labels = Tabs.entries.map { it.labelRes }
        val icons = Tabs.entries.map { it.icon }
        assertEquals(labels.size, labels.toSet().size)
        assertEquals(icons.size, icons.toSet().size)
    }
}
