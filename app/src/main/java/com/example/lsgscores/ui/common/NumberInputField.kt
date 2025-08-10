package com.example.lsgscores.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minValue: Int = 0,
    maxValue: Int? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Only allow digits
            onValueChange(newValue.filter { it.isDigit() })
        },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp) // Spacing from right border
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    IconButton(
                        onClick = {
                            val currentValue = value.toIntOrNull() ?: 0
                            if (currentValue > minValue) {
                                onValueChange((currentValue - 1).toString())
                            }
                        },
                        modifier = Modifier.size(28.dp),
                        enabled = (value.toIntOrNull() ?: 0) > minValue
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    IconButton(
                        onClick = {
                            val currentValue = value.toIntOrNull() ?: 0
                            val newValue = currentValue + 1
                            if (maxValue == null || newValue <= maxValue) {
                                onValueChange(newValue.toString())
                            }
                        },
                        modifier = Modifier.size(28.dp),
                        enabled = maxValue?.let { (value.toIntOrNull() ?: 0) < it } ?: true
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        })
}