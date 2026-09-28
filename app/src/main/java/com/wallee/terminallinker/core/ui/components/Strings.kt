package com.wallee.terminallinker.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Thin alias so components read a string resource without importing `androidx.compose.ui.res` everywhere. */
@Composable
internal fun stringResourceCompat(@StringRes id: Int): String = stringResource(id)
