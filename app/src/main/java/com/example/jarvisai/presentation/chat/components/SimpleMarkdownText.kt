package com.example.jarvisai.presentation.chat.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisCodeBackground
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.delay

/**
 * Clean, lightweight Markdown parser for Compose.
 * Handles:
 * - Code blocks: ```code``` with line numbers and copy feedback
 * - Inline code: `code`
 * - Bold: **text**
 * - Italic: *text*
 * - Headers: ### Header
 * - Neon cursor indicator when isStreaming is true
 */
@Composable
fun SimpleMarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = JarvisTextPrimary,
    fontSize: Int = 15,
    isStreaming: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neon_cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    SelectionContainer {
        Column(modifier = modifier) {
            val parts = remember(content) { splitIntoBlocks(content) }
            val lastIndex = parts.lastIndex

            for ((index, block) in parts.withIndex()) {
                val isLastBlock = index == lastIndex
                when (block) {
                    is MarkdownBlock.CodeBlock -> {
                        EnhancedCodeBlockView(
                            block = block,
                            fontSize = fontSize
                        )
                    }
                    is MarkdownBlock.Paragraph -> {
                        val annotated = remember(block.text, textColor) {
                            parseInlineMarkdown(block.text, textColor)
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = annotated,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize + 7).sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isStreaming && isLastBlock) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "▍",
                                    color = JarvisPrimary,
                                    fontSize = (fontSize + 1).sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.alpha(cursorAlpha)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedCodeBlockView(
    block: MarkdownBlock.CodeBlock,
    fontSize: Int
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val lines = remember(block.code) { block.code.lines() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisCodeBackground)
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Language badge + Animated Copy Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1B2A))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = JarvisPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = (block.language.ifBlank { "código" }).uppercase(),
                        color = JarvisPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Copy button with feedback
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isCopied) Color(0xFF00363A) else Color(0xFF17283C))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(block.code))
                            isCopied = true
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (isCopied) "Copiado" else "Copiar código",
                        tint = if (isCopied) JarvisAccentGreen else JarvisTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isCopied) "¡COPIADO!" else "COPIAR",
                        color = if (isCopied) JarvisAccentGreen else JarvisTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Code Content with Line Numbers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Line numbers gutter
                Column(
                    modifier = Modifier
                        .background(Color(0xFF0A131F))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lines.size) {
                        Text(
                            text = "$i",
                            color = Color(0xFF4A6572),
                            fontFamily = FontFamily.Monospace,
                            fontSize = (fontSize - 3).sp,
                            lineHeight = (fontSize + 3).sp
                        )
                    }
                }

                // Code lines
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    for (line in lines) {
                        Text(
                            text = if (line.isEmpty()) " " else line,
                            color = JarvisPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (fontSize - 2).sp,
                            lineHeight = (fontSize + 3).sp
                        )
                    }
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String = "") : MarkdownBlock
}

private fun splitIntoBlocks(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = content.lines()
    var inCodeBlock = false
    val codeAccumulator = StringBuilder()
    var codeLang = ""
    val paragraphAccumulator = StringBuilder()

    for (line in lines) {
        if (line.trimStart().startsWith("```")) {
            if (inCodeBlock) {
                // End code block
                blocks.add(MarkdownBlock.CodeBlock(codeAccumulator.toString().trimEnd(), codeLang))
                codeAccumulator.clear()
                codeLang = ""
                inCodeBlock = false
            } else {
                // Start code block: flush previous paragraph
                if (paragraphAccumulator.isNotBlank()) {
                    blocks.add(MarkdownBlock.Paragraph(paragraphAccumulator.toString().trimEnd()))
                    paragraphAccumulator.clear()
                }
                codeLang = line.trimStart().removePrefix("```").trim()
                inCodeBlock = true
            }
        } else if (inCodeBlock) {
            codeAccumulator.append(line).append("\n")
        } else {
            paragraphAccumulator.append(line).append("\n")
        }
    }

    if (inCodeBlock && codeAccumulator.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeAccumulator.toString().trimEnd(), codeLang))
    } else if (paragraphAccumulator.isNotBlank()) {
        blocks.add(MarkdownBlock.Paragraph(paragraphAccumulator.toString().trimEnd()))
    }

    return if (blocks.isEmpty()) listOf(MarkdownBlock.Paragraph(content)) else blocks
}

private fun parseInlineMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            when {
                // Bold: **text**
                i + 1 < len && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline Code: `code`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = JarvisPrimary,
                                background = JarvisCodeBackground
                            )
                        )
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *text*
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Header at line start: ###
                text.startsWith("### ", i) -> {
                    val endOfLine = text.indexOf('\n', i)
                    val headerText = if (endOfLine != -1) text.substring(i + 4, endOfLine) else text.substring(i + 4)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JarvisPrimary))
                    append(headerText)
                    pop()
                    i = if (endOfLine != -1) endOfLine else len
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
