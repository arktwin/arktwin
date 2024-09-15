import { MaterialReactTable, useMaterialReactTable, type MRT_ColumnDef } from 'material-react-table'
import React, { useEffect, useMemo, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { Box, MapControls, OrthographicCamera } from '@react-three/drei'
import { Canvas } from '@react-three/fiber'
import { RowSelectionState } from '@tanstack/react-table'

import { EdgeNeighborsQueryResponse, Transform } from './types'

createRoot(document.getElementById('app')!).render(<App />)

class Agent {
  constructor(
    public id: string,
    public kind: string,
    public transform: Transform,
  ) {}
}

function App(): JSX.Element {
  const [selectedAgentIds, setSelectedAgentIds] = useState<Array<string>>([])
  const [neighborsNumber, setNeighborsNumber] = useState<number>(1000)
  const [agents, updateAgents] = useState<Agent[]>([])

  useEffect(() => {
    const timer = setInterval(() => {
      fetch('/api/edge/neighbors/_query', {
        method: 'POST',
        body: JSON.stringify({
          neighborsNumber: neighborsNumber,
          changeDetection: false,
        }),
      })
        .then((response) => response.json())
        .then((data: EdgeNeighborsQueryResponse) => {
          updateAgents(
            Object.entries(data.neighbors)
              .filter(([_id, agent]) => agent.kind !== null && agent.transform !== null)
              .map(([id, agent]) => new Agent(id, agent.kind!, agent.transform!)),
          )
        })
    }, 1000)
    return () => {
      clearInterval(timer)
    }
  }, [neighborsNumber])

  return (
    <div>
      <div
        style={{
          float: 'left',
          width: '640px',
          height: '100vh',
          overflowY: 'scroll',
        }}
      >
        <div style={{ margin: '16px', textAlign: 'center' }}>
          <RequestInput neighborsNumber={neighborsNumber} setNeighborsNumber={setNeighborsNumber} />
        </div>
        <div style={{ margin: '16px' }}>
          <AgentsTable
            agents={agents}
            selectedAgentIds={selectedAgentIds}
            setSelectedAgentIds={setSelectedAgentIds}
          />
        </div>
      </div>
      <div
        style={{
          float: 'left',
          left: '640px',
          width: 'calc(100% - 640px)',
          height: '100vh',
        }}
      >
        <div style={{ margin: '16px' }}>
          <AgentsCanvas agents={agents} selectedAgentIds={selectedAgentIds} />
        </div>
      </div>
    </div>
  )
}

function RequestInput(props: {
  neighborsNumber: number
  setNeighborsNumber: (a: number) => void
}): JSX.Element {
  return (
    <label>
      neighbors number{' '}
      <input
        type="number"
        value={props.neighborsNumber}
        onChange={(event) => props.setNeighborsNumber(event.target.valueAsNumber)}
      />
    </label>
  )
}

function AgentsTable(props: {
  agents: Array<Agent>
  selectedAgentIds: Array<string>
  setSelectedAgentIds: (a: Array<string>) => void
}): JSX.Element {
  const [rowSelection, setRowSelection] = useState<RowSelectionState>({})

  const columns = useMemo<MRT_ColumnDef<Agent>[]>(
    () => [
      {
        accessorKey: 'id',
        header: 'id',
        size: 100,
      },
      {
        accessorKey: 'kind',
        header: 'kind',
        size: 100,
      },
      {
        accessorKey: 'transform.localTranslation.x',
        header: 'x',
        size: 50,
      },
      {
        accessorKey: 'transform.localTranslation.y',
        header: 'y',
        size: 50,
      },
      {
        accessorKey: 'transform.localTranslation.z',
        header: 'z',
        size: 50,
      },
    ],
    [],
  )

  const table = useMaterialReactTable({
    columns,
    data: props.agents,
    getRowId: (row) => row.id,
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    state: {
      rowSelection,
    },
  })

  useEffect(() => {
    props.setSelectedAgentIds(table.getSelectedRowModel().flatRows.map((row) => row.id))
  }, [rowSelection, table])

  return <MaterialReactTable table={table} />
}

function AgentsCanvas(props: {
  agents: Array<Agent>
  selectedAgentIds: Array<string>
}): JSX.Element {
  const maxAbsXYe = Math.pow(
    10,
    Math.floor(
      Math.log10(
        Math.max(
          ...props.agents.map((agent) => Math.abs(agent.transform.localTranslation.x)),
          ...props.agents.map((agent) => Math.abs(agent.transform.localTranslation.y)),
        ),
      ),
    ),
  )

  return (
    <Canvas style={{ width: '100%', height: 'calc(100vh - 32px)' }}>
      <OrthographicCamera
        makeDefault
        position={[0, Number.MAX_SAFE_INTEGER / 10, 0]}
        left={-maxAbsXYe * 11}
        right={maxAbsXYe * 11}
        bottom={-maxAbsXYe * 11}
        top={maxAbsXYe * 11}
        far={Number.MAX_SAFE_INTEGER}
      />
      <MapControls />
      <ambientLight />
      <gridHelper args={[maxAbsXYe * 20, 20, 0x00ffff, 0x00ff00]} />
      {props.agents.map((agent) => {
        const v = agent.transform.localTranslation
        return props.selectedAgentIds.includes(agent.id) ? (
          <Box
            key={agent.id}
            position={[v.x, v.z, -v.y]}
            args={[maxAbsXYe / 7, maxAbsXYe / 7, maxAbsXYe / 7]}
          >
            <meshStandardMaterial color="red" />
          </Box>
        ) : (
          <Box
            key={agent.id}
            position={[v.x, v.z, -v.y]}
            args={[maxAbsXYe / 10, maxAbsXYe / 10, maxAbsXYe / 10]}
          >
            <meshStandardMaterial color="black" />
          </Box>
        )
      })}
    </Canvas>
  )
}
