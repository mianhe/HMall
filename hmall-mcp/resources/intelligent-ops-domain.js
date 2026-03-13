/**
 * 智能运营领域知识 Resource。
 * 内容维护在 docs/intelligent-ops-ontology.md，此文件仅负责加载并注册为 MCP Resource。
 */
import { readFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ONTOLOGY_PATH = resolve(__dirname, '../../docs/intelligent-ops-ontology.md')

export const INTELLIGENT_OPS_DOMAIN_URI = 'hmall://intelligent-ops/domain-knowledge'

const ontologyText = readFileSync(ONTOLOGY_PATH, 'utf-8')

export function registerIntelligentOpsResources(server) {
  server.resource(
    'intelligent-ops-domain-knowledge',
    INTELLIGENT_OPS_DOMAIN_URI,
    {
      description: '智能运营领域知识：事件类型语义、Payload 结构、业务流程状态机、因果链、健康指标推导、多维查询指南。',
      mimeType: 'text/plain',
    },
    async () => ({
      contents: [{ uri: INTELLIGENT_OPS_DOMAIN_URI, mimeType: 'text/plain', text: ontologyText }],
    }),
  )
}
