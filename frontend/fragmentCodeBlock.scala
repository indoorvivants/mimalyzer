package mimalyzer.frontend

import com.raquo.laminar.api.L.*

def codeBlock(language: String, value: String) =
  pre(
    code(
      cls := s"language-$language",
      cls := "static-code-block",
      onMountCallback(mnt => hljs.highlightElement(mnt.thisNode.ref)),
      value
    )
  )
