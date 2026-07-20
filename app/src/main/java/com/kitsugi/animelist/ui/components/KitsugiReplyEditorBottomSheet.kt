package com.kitsugi.animelist.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch

enum class MarkdownTag(val syntax: String, val offset: Int, val label: String, val icon: ImageVector) {
    BOLD("**text**", 2, "Kalın", Icons.Rounded.FormatBold),
    ITALIC("*text*", 1, "Eğik", Icons.Rounded.FormatItalic),
    STRIKETHROUGH("~~text~~", 2, "Üstü Çizili", Icons.Rounded.FormatStrikethrough),
    SPOILER("~!text!~", 2, "Spoiler", Icons.Rounded.VisibilityOff),
    CENTER("~~~text~~~", 3, "Ortala", Icons.Rounded.FormatAlignCenter),
    LINK("[text](url)", 1, "Bağlantı", Icons.Rounded.Link),
    IMAGE("img(url)", 1, "Görsel", Icons.Rounded.Image),
    YOUTUBE("youtube(url)", 1, "YouTube", Icons.Rounded.PlayCircle),
    ORDERED_LIST("\n1. text", 0, "Numaralı Liste", Icons.Rounded.FormatListNumbered),
    UNORDERED_LIST("\n- text", 0, "Madde Listesi", Icons.Rounded.FormatListBulleted),
    QUOTE("\n> text", 0, "Alıntı", Icons.Rounded.FormatQuote),
    CODE("`text`", 1, "Kod", Icons.Rounded.Code)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiReplyEditorBottomSheet(
    title: String = "Yanıtla",
    placeholder: String = "Bir şeyler yaz...",
    onPublish: suspend (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    
    var textValue by remember { mutableStateOf(TextFieldValue("")) }
    var isPublishing by remember { mutableStateOf(false) }
    
    KitsugiSheetOrDialog(
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = KitsugiColors.SurfaceStrong
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = KitsugiColors.TextPrimary
                    )
                }

                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        if (textValue.text.isBlank()) {
                            Toast.makeText(context, "Metin boş olamaz", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isPublishing = true
                        coroutineScope.launch {
                            val success = onPublish(textValue.text)
                            isPublishing = false
                            if (success) {
                                Toast.makeText(context, "Başarıyla gönderildi", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Gönderilemedi, lütfen giriş durumunuzu kontrol edin", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    enabled = !isPublishing
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = KitsugiColors.Background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Yayınla",
                            color = KitsugiColors.Background,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text input area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(KitsugiColors.SurfaceStrong, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = KitsugiColors.TextPrimary, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    decorationBox = { innerTextField ->
                        if (textValue.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = KitsugiColors.TextMuted,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Formatting bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MarkdownTag.values().forEach { tag ->
                    IconButton(
                        onClick = {
                            val text = textValue.text
                            val selection = textValue.selection
                            val before = text.substring(0, selection.start)
                            val selected = text.substring(selection.start, selection.end)
                            val after = text.substring(selection.end)

                            val replacement = if (selected.isNotEmpty()) {
                                tag.syntax.replace("text", selected).replace("url", "")
                            } else {
                                tag.syntax
                            }

                            val newText = before + replacement + after
                            val newCursorPos = before.length + replacement.length - tag.offset
                            
                            textValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursorPos)
                            )
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = KitsugiColors.SurfaceStrong
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = tag.icon,
                            contentDescription = tag.label,
                            tint = KitsugiColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
