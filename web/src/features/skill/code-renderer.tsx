import { useMemo, type ReactNode } from 'react'
import { common, createLowlight } from 'lowlight'

// Create lowlight instance with common languages
const lowlight = createLowlight(common)
type HighlightedTree = ReturnType<(typeof lowlight)['highlight']>
type HighlightedNode = HighlightedTree | HighlightedTree['children'][number]

interface CodeRendererProps {
  code: string
  language: string | null
  className?: string
}

/**
 * Renders code with syntax highlighting using lowlight (highlight.js wrapper).
 * Reuses the same styling as Markdown code blocks for visual consistency.
 * Memoized to prevent re-highlighting on every render.
 */
export function CodeRenderer({ code, language, className }: CodeRendererProps) {
  // Cache syntax highlighting result
  const highlightedCode = useMemo(() => {
    try {
      if (language && lowlight.registered(language)) {
        const tree = lowlight.highlight(language, code, { prefix: 'hljs-' })
        return renderHighlightedNode(tree)
      }
      return code
    } catch (error) {
      console.error('Syntax highlighting failed:', error)
      return code
    }
  }, [code, language])

  return (
    <div className={className}>
      {/* Simplified styling - removed gradient, blur, and shadow for better performance */}
      <div className="my-4 rounded-lg border border-border/60 bg-secondary/30">
        <div className="max-w-full overflow-x-auto rounded-lg bg-background px-4 py-3">
          <pre className="m-0 min-w-max bg-transparent p-0 text-[13px] leading-6">
            <code className="hljs">{highlightedCode}</code>
          </pre>
        </div>
      </div>
    </div>
  )
}

/**
 * Keep package source as React text, including inside syntax-highlight spans.
 */
function renderHighlightedNode(node: HighlightedNode, key = 0): ReactNode {
  if (node.type === 'text') {
    return node.value
  }

  if (node.type === 'element') {
    const classNames = node.properties?.className
    const className = Array.isArray(classNames) ? classNames.join(' ') : ''
    return <span key={key} className={className}>{node.children.map(renderHighlightedNode)}</span>
  }

  if (node.type === 'root') {
    return node.children.map(renderHighlightedNode)
  }

  return null
}
