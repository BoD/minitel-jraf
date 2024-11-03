/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2024-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.miniteljraf.util

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.RecursiveVisitor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

private class PlainTextVisitor(private val readmeText: String) : RecursiveVisitor() {
  val plainText = StringBuilder()
  var level = 0

  override fun visitNode(node: ASTNode) {
//    println(level.toString().repeat(level) + " " + node.type.toString() + " " + node.getTextInNode(readmeText))
    when (node) {
      is CompositeASTNode -> {
        when (node.type) {
          MarkdownElementTypes.ATX_1 -> {
            val content = node.recursiveFindChildOfType(MarkdownTokenTypes.ATX_CONTENT)!!.getTextInNode(readmeText)
            plainText.append("# $content")
          }

          MarkdownElementTypes.ATX_2 -> {
            val content = node.recursiveFindChildOfType(MarkdownTokenTypes.ATX_CONTENT)!!.getTextInNode(readmeText)
            plainText.append("## $content")
          }

          MarkdownElementTypes.ATX_3 -> {
            val content = node.recursiveFindChildOfType(MarkdownTokenTypes.ATX_CONTENT)!!.getTextInNode(readmeText)
            plainText.append("### $content")
          }

          MarkdownElementTypes.SETEXT_1 -> {
            val content = node.recursiveFindChildOfType(MarkdownTokenTypes.SETEXT_CONTENT)!!.getTextInNode(readmeText)
            plainText.append("# $content")
          }

          MarkdownElementTypes.SETEXT_2 -> {
            val content = node.recursiveFindChildOfType(MarkdownTokenTypes.SETEXT_CONTENT)!!.getTextInNode(readmeText)
            plainText.append("## $content")
          }

          MarkdownElementTypes.IMAGE,
            -> {
          }

          MarkdownElementTypes.INLINE_LINK -> {
            val linkTextNode = node.recursiveFindChildOfType(MarkdownElementTypes.LINK_TEXT)!!.children[1]
            level++
            visitNode(linkTextNode)
            level--
          }

          else -> {
            level++
            super.visitNode(node)
            level--
          }
        }
      }

      is LeafASTNode -> {
        when (node.type) {
          MarkdownTokenTypes.BACKTICK,
          MarkdownTokenTypes.CODE_FENCE_START,
          MarkdownTokenTypes.FENCE_LANG,
          MarkdownTokenTypes.CODE_FENCE_END,
            -> {
          }

          else -> {
            plainText.append(node.getTextInNode(readmeText))
          }
        }
      }
    }
  }
}

private fun ASTNode.recursiveFindChildOfType(type: IElementType): ASTNode? {
  if (this.type == type) {
    return this
  }
  for (child in children) {
    val result = child.recursiveFindChildOfType(type)
    if (result != null) {
      return result
    }
  }
  return null
}

fun String.toPlainText(): String {
  val markdownAst = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(this)
  val plainTextVisitor = PlainTextVisitor(this)
  markdownAst.accept(plainTextVisitor)
  return plainTextVisitor.plainText.toString().replace(Regex("\\n{3,}"), "\n\n")
}
