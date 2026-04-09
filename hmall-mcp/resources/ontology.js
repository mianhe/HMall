/**
 * HMall 电商本体（Ontology）— 系统级统一业务模型。
 * 内容维护在 docs/ontology/hmall-ontology.md，此文件仅负责加载并注册为 MCP Resource。
 */
import { readFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ONTOLOGY_PATH = resolve(__dirname, '../../docs/ontology/hmall-ontology.md')

export const ONTOLOGY_URI = 'hmall://ontology/schema'

const ontologyText = readFileSync(ONTOLOGY_PATH, 'utf-8')

export function registerOntologyResources(server) {
  server.resource(
    'ontology-schema',
    ONTOLOGY_URI,
    {
      description: 'HMall 电商本体：系统级统一业务模型，定义所有核心对象、关联关系、MCP 工具映射和领域事件。AI 理解系统的统一入口。',
      mimeType: 'text/plain',
    },
    async () => ({
      contents: [{ uri: ONTOLOGY_URI, mimeType: 'text/plain', text: ontologyText }],
    }),
  )
}
