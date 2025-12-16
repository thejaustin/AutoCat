package app.lawnchair.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun <IN, OUT> rememberTransformAdapter(
    adapter: PreferenceAdapter<IN>,
    transformGet: (IN) -> OUT,
    transformSet: (OUT) -> IN,
): PreferenceAdapter<OUT> {
    val currentValue by adapter.state
    val transformedValue = remember(currentValue) { transformGet(currentValue) }

    return remember(adapter) {
        object : PreferenceAdapter<OUT> {
            override val state: State<OUT> = object : State<OUT> {
                override val value: OUT
                    get() = transformedValue
            }

            override fun onChange(newValue: OUT) {
                adapter.onChange(transformSet(newValue))
            }
        }
    }
}
