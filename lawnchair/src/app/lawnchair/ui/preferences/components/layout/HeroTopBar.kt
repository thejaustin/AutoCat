package app.lawnchair.ui.preferences.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Enhanced large top app bar with hero styling.
 * Features larger typography, optional hero images, and gradient backgrounds.
 *
 * @param title The title text to display
 * @param scrollBehavior Scroll behavior for collapsing animation
 * @param heroImage Optional composable for hero imagery (shown above title)
 * @param gradient Optional gradient brush for background
 * @param navigationIcon Navigation icon composable (typically back button)
 * @param actions Action icons composable (typically overflow menu)
 * @param modifier Modifier for the top bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroLargeTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    heroImage: (@Composable () -> Unit)? = null,
    gradient: Brush? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val containerColor = if (gradient != null) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Optional hero image section
        heroImage?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        gradient ?: SolidColor(MaterialTheme.colorScheme.surfaceContainer),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                it()
            }
        }

        // Large top app bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient ?: SolidColor(containerColor)),
        ) {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            )
        }
    }
}

/**
 * Medium-sized hero top app bar.
 * Balance between standard and large variants.
 *
 * @param title The title text to display
 * @param scrollBehavior Scroll behavior for collapsing animation
 * @param gradient Optional gradient brush for background
 * @param navigationIcon Navigation icon composable
 * @param actions Action icons composable
 * @param modifier Modifier for the top bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroMediumTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    gradient: Brush? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val containerColor = if (gradient != null) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient ?: SolidColor(containerColor)),
    ) {
        androidx.compose.material3.MediumTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}

/**
 * Creates a gradient brush for hero headers.
 * Provides expressive background coloring.
 *
 * @param primaryColor Primary color for the gradient
 * @param secondaryColor Secondary color for the gradient
 * @param alpha Overall opacity (default: 0.15f for subtle effect)
 */
@Composable
fun heroGradient(
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    alpha: Float = 0.15f,
): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            primaryColor.copy(alpha = alpha),
            secondaryColor.copy(alpha = alpha * 0.5f),
            Color.Transparent,
        ),
    )
}

/**
 * Creates a gradient brush for hero headers using surface containers.
 * More subtle than colorful gradients.
 *
 * @param alpha Overall opacity (default: 0.5f)
 */
@Composable
fun heroSurfaceGradient(
    alpha: Float = 0.5f,
): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha),
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha * 0.7f),
            Color.Transparent,
        ),
    )
}
