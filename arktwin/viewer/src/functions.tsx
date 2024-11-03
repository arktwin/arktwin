import { ToggleButton, ToggleButtonGroup } from '@mui/material'
import { Box, MapControls } from '@react-three/drei'
import { Canvas, useFrame, useThree } from '@react-three/fiber'
import {
  type MRT_ColumnDef,
  type MRT_RowSelectionState,
  MaterialReactTable,
  useMaterialReactTable,
} from 'material-react-table'
import { useEffect, useMemo, useState } from 'react'
import { Euler, OrthographicCamera } from 'three'
import type { EdgeNeighborsQueryResponse } from './types.tsx'

class Agent {
  constructor(
    readonly id: string,
    readonly kind: string,
    readonly x: number,
    readonly y: number,
    readonly z: number,
  ) {}
}

export function App(): JSX.Element {
  const [selectedAgentIds, setSelectedAgentIds] = useState<string[]>([])
  const [agents, updateAgents] = useState<Agent[]>([])
  const [projectionAxes, setProjectionAxes] = useState<'xy' | 'yz' | 'xz'>('xy')

  useEffect(() => {
    const timer = setInterval(() => {
      fetch('/api/edge/neighbors/_query', {
        method: 'POST',
        body: JSON.stringify({
          changeDetection: false,
        }),
      })
        .then((response) => response.json())
        .then((data: EdgeNeighborsQueryResponse) => {
          updateAgents(
            Object.entries(data.neighbors).flatMap(([id, neighbor]) => {
              if (neighbor.kind != null && neighbor.transform != null) {
                const v = neighbor.transform.localTranslation
                return [new Agent(id, neighbor.kind, v.x, v.y, v.z)]
              }
              return []
            }),
          )
        })
    }, 1000)
    return () => {
      clearInterval(timer)
    }
  }, [])

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
          width: 'calc(100% - 660px)',
          height: '100vh',
          textAlign: 'center',
        }}
      >
        <span style={{ marginRight: '16px' }}>Projection</span>
        <ToggleButtonGroup
          color="primary"
          value={projectionAxes}
          onChange={(_event, value) => {
            if (value != null) {
              setProjectionAxes(value)
            }
          }}
          exclusive={true}
        >
          <ToggleButton value="xy">XY</ToggleButton>
          <ToggleButton value="yz">YZ</ToggleButton>
          <ToggleButton value="xz">XZ</ToggleButton>
        </ToggleButtonGroup>
        <div style={{ margin: '16px', height: 'calc(100vh - 96px)' }}>
          <AgentsCanvas
            agents={agents}
            selectedAgentIds={selectedAgentIds}
            projectionAxes={projectionAxes}
          />
        </div>
      </div>
    </div>
  )
}

function AgentsTable(props: {
  agents: Agent[]
  selectedAgentIds: string[]
  setSelectedAgentIds: (a: string[]) => void
}): JSX.Element {
  const [rowSelection, setRowSelection] = useState<MRT_RowSelectionState>({})

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
        accessorKey: 'x',
        header: 'x',
        size: 50,
      },
      {
        accessorKey: 'y',
        header: 'y',
        size: 50,
      },
      {
        accessorKey: 'z',
        header: 'z',
        size: 50,
      },
    ],
    [],
  )

  const data = useMemo<Agent[]>(
    () =>
      props.agents.map(
        (a) =>
          new Agent(a.id, a.kind, toPrecision(a.x, 5), toPrecision(a.y, 5), toPrecision(a.z, 5)),
      ),
    [props.agents],
  )

  const table = useMaterialReactTable({
    columns,
    data: data,
    getRowId: (row) => row.id,
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    state: {
      rowSelection,
    },
  })

  const { setSelectedAgentIds } = props
  useEffect(() => {
    setSelectedAgentIds(
      Object.entries(rowSelection)
        .filter(([_key, value]) => value)
        .map(([key]) => key),
    )
  }, [setSelectedAgentIds, rowSelection])

  return <MaterialReactTable table={table} />
}

function AgentsCanvas(props: {
  agents: Agent[]
  selectedAgentIds: string[]
  projectionAxes: 'xy' | 'yz' | 'xz'
}): JSX.Element {
  const { agents, projectionAxes } = props
  const gridSize = useMemo<number>(
    () =>
      10 **
      Math.ceil(
        Math.log10(
          Math.max(
            ...(['xy', 'xz'].includes(projectionAxes)
              ? agents.map((agent) => Math.abs(agent.x))
              : []),
            ...(['xy', 'yz'].includes(projectionAxes)
              ? agents.map((agent) => Math.abs(agent.y))
              : []),
            ...(['yz', 'xz'].includes(projectionAxes)
              ? agents.map((agent) => Math.abs(agent.z))
              : []),
          ),
        ),
      ),
    [agents, projectionAxes],
  )

  const girdRoation = (() => {
    switch (props.projectionAxes) {
      case 'xy':
        return new Euler(Math.PI / 2, 0, 0)
      case 'yz':
        return new Euler(0, 0, Math.PI / 2)
      case 'xz':
        return new Euler(0, 0, 0)
      default:
        return new Euler(0, 0, 0)
    }
  })()

  return (
    <Canvas>
      <Camera gridSize={gridSize} projectionAxes={props.projectionAxes} />
      <MapControls />
      <ambientLight />
      <gridHelper args={[gridSize * 2, 20, 0x00ffff, 0x00ff00]} rotation={girdRoation} />
      {props.agents.map((agent) => {
        return props.selectedAgentIds.includes(agent.id) ? (
          <Box
            key={agent.id}
            position={[agent.x, agent.y, agent.z]}
            args={[gridSize / 70, gridSize / 70, gridSize / 70]}
          >
            <meshStandardMaterial color="red" />
          </Box>
        ) : (
          <Box
            key={agent.id}
            position={[agent.x, agent.y, agent.z]}
            args={[gridSize / 100, gridSize / 100, gridSize / 100]}
          >
            <meshStandardMaterial color="black" />
          </Box>
        )
      })}
    </Canvas>
  )
}

function Camera(props: {
  gridSize: number
  projectionAxes: 'xy' | 'yz' | 'xz'
}): JSX.Element {
  const { gridSize, projectionAxes } = props

  // see https://r3f.docs.pmnd.rs/api/hooks#exchanging-defaults
  const setThreeState = useThree((state) => state.set)

  useEffect(() => {
    const camera = new OrthographicCamera(
      -gridSize * 1.1,
      gridSize * 1.1,
      gridSize * 1.1,
      -gridSize * 1.1,
      0,
      Number.MAX_SAFE_INTEGER / 5,
    )
    switch (projectionAxes) {
      case 'xy': {
        camera.position.set(0, 0, Number.MAX_SAFE_INTEGER / 10)
        camera.lookAt(0, 0, 0)
        break
      }
      case 'yz': {
        camera.position.set(Number.MAX_SAFE_INTEGER / 10, 0, 0)
        camera.lookAt(0, 0, 0)
        break
      }
      case 'xz': {
        camera.position.set(0, Number.MAX_SAFE_INTEGER / 10, 0)
        camera.lookAt(0, 0, 0)
        camera.scale.y = -1
        break
      }
      default:
        break
    }
    setThreeState({ camera })
  }, [gridSize, projectionAxes, setThreeState])

  useFrame((state) => {
    switch (projectionAxes) {
      case 'xy':
        break
      case 'yz':
        // the camera rotation resets to (0, Math.PI / 2, 0) every frame for unkonwn reasons
        state.camera.rotation.set(Math.PI / 2, Math.PI / 2, 0)
        break
      case 'xz':
        break
      default:
        break
    }
  })
  return <></>
}

function toPrecision(num: number, precision: number): number {
  return Number.parseFloat(num.toPrecision(precision))
}
