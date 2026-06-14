package app.lawnchair.ui.preferences.components.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.animations.pressableScale
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.haptics.PreferenceHapticType
import app.lawnchair.ui.preferences.haptics.rememberPreferenceHaptics

/**
 * Button style for ButtonPreference.
 */
enum class ButtonVariant {
    /** Standard filled button with primary color */
    FILLED,

    /** Filled tonal button with secondary container color */
    FILLED_TONAL,

    /** Outlined button with border */
    OUTLINED,

    /** Text-only button */
    TEXT,
}

/**
 * A preference that displays as an action button.
 * Useful for triggering actions or navigating to screens.
 *
 * @param title The title/label of the preference
 * @param description Optional description text shown above the button
 * @param buttonText The text displayed on the button
 * @param icon Optional icon for the button
 * @param variant The button style variant (default: FILLED_TONAL)
 * @param onClick Callback when the button is clicked
 * @param enabled Whether the button is enabled
 * @param modifier Modifier for the preference
 */
@Composable
fun ButtonPreference(
    title: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    variant: ButtonVariant = ButtonVariant.FILLED_TONAL,
    enabled: Boolean = true,
) {
    val haptics = rememberPreferenceHaptics()

    PreferenceTemplate(
        title = { Text(text = title) },
        description = { description?.let { Text(text = it) } },
        endWidget = {
            when (variant) {
                ButtonVariant.FILLED -> Button(
                    onClick = {
                        haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                        onClick()
                    },
                    enabled = enabled,
                    modifier = Modifier.pressableScale(enabled = enabled),
                ) {
                    ButtonContent(buttonText, icon)
                }

                ButtonVariant.FILLED_TONAL -> FilledTonalButton(
                    onClick = {
                        haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                        onClick()
                    },
                    enabled = enabled,
                    modifier = Modifier.pressableScale(enabled = enabled),
                ) {
                    ButtonContent(buttonText, icon)
                }

                ButtonVariant.OUTLINED -> OutlinedButton(
                    onClick = {
                        haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                        onClick()
                    },
                    enabled = enabled,
                    modifier = Modifier.pressableScale(enabled = enabled),
                ) {
                    ButtonContent(buttonText, icon)
                }

                ButtonVariant.TEXT -> TextButton(
                    onClick = {
                        haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                        onClick()
                    },
                    enabled = enabled,
                    modifier = Modifier.pressableScale(enabled = enabled),
                ) {
                    ButtonContent(buttonText, icon)
                }
            }
        },
        modifier = modifier,
        applyPaddings = true,
    )
}

/**
 * Full-width button preference that spans the entire row.
 * Useful for primary actions.
 *
 * @param text The button text
 * @param icon Optional icon
 * @param variant The button style (default: FILLED_TONAL)
 * @param onClick Callback when clicked
 * @param enabled Whether enabled
 * @param modifier Modifier for the button
 */
@Composable
fun FullWidthButtonPreference(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: ButtonVariant = ButtonVariant.FILLED_TONAL,
    enabled: Boolean = true,
) {
    val haptics = rememberPreferenceHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (variant) {
            ButtonVariant.FILLED -> Button(
                onClick = {
                    haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                    onClick()
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressableScale(enabled = enabled),
            ) {
                ButtonContent(text, icon)
            }

            ButtonVariant.FILLED_TONAL -> FilledTonalButton(
                onClick = {
                    haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                    onClick()
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressableScale(enabled = enabled),
            ) {
                ButtonContent(text, icon)
            }

            ButtonVariant.OUTLINED -> OutlinedButton(
                onClick = {
                    haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                    onClick()
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressableScale(enabled = enabled),
            ) {
                ButtonContent(text, icon)
            }

            ButtonVariant.TEXT -> TextButton(
                onClick = {
                    haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                    onClick()
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressableScale(enabled = enabled),
            ) {
                ButtonContent(text, icon)
            }
        }
    }
}

/**
 * Internal composable for button content with optional icon.
 */
@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
