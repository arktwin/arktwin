export type EdgeNeighborsQueryResponse = {
  timestamp: object
  neighbors: { [key: string]: EdgeNeighborsQueryResponseAgent }
}

export type EdgeNeighborsQueryResponseAgent = {
  transform?: Transform
  nearestDistance?: number
  kind?: string
  status?: object
  assets?: object
  change?: string
}

export type Transform = {
  parentAgentId?: string
  globalScale: Vector3
  localRotation: object
  localTranslation: Vector3
  localTranslationSpeed?: Vector3
}

export type Vector3 = {
  x: number
  y: number
  z: number
}
