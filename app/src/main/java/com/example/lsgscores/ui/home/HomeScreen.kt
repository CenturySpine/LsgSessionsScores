package com.example.lsgscores.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lsgscores.R

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(300.dp)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)

                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.lsg_vert_banner),
                        contentDescription = "App logo",
                        modifier = Modifier.fillMaxSize()
                    )

                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Welcome to LSG scores !",
                style = MaterialTheme.typography.headlineMedium
            )

        }
    }

}
