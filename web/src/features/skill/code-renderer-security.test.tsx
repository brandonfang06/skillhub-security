/** @vitest-environment jsdom */
import { cleanup, render } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { CodeRenderer } from './code-renderer'

afterEach(cleanup)

describe('CodeRenderer untrusted package source', () => {
  it.each([null, 'html', 'javascript'])('renders executable markup as text with language %s', (language) => {
    const source = '<img src=x onerror=alert(1)><script>alert("xss")</script> & "quoted"'
    const { container } = render(<CodeRenderer code={source} language={language} />)

    expect(container.querySelector('code')?.textContent).toBe(source)
    expect(container.querySelector('img, script, [onerror]')).toBeNull()
  })

  it('preserves syntax highlighting without parsing the source as HTML', () => {
    const { container } = render(<CodeRenderer code="const answer = 42" language="javascript" />)
    expect(container.querySelector('code')?.textContent).toBe('const answer = 42')
    expect(container.querySelector('.hljs-keyword')?.textContent).toBe('const')
  })
})
