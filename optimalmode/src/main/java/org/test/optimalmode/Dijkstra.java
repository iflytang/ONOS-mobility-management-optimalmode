package org.test.optimalmode;

/**
 * Created by tsf on 4/18/17.
 *
 * @Description Dijkstra Algorithm to get shortest path.
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class Dijkstra {

    //========================= test correctness of algorithm ========================
  /*  public static void main(String[] args) {
        Graph g = new Graph();
		*//*g.addVertex('1', Arrays.asList(new Vertex('4', 1)));
		g.addVertex('2', Arrays.asList(new Vertex('3', 1)));
		g.addVertex('3', Arrays.asList(new Vertex('2', 1), new Vertex('4', 1), new Vertex('5', 1)));
		g.addVertex('4', Arrays.asList(new Vertex('1', 1), new Vertex('3', 1), new Vertex('5', 1)));
		g.addVertex('5', Arrays.asList(new Vertex('3', 1), new Vertex('4', 1), new Vertex('6', 1)));
		g.addVertex('6', Arrays.asList(new Vertex('5', 1)));*//*
//		Vertex vertex1 = new Vertex(1,1);Vertex vertex2 = new Vertex(2,1);Vertex vertex3 = new Vertex(3,1);
//		Vertex vertex4 = new Vertex(4,1);Vertex vertex5 = new Vertex(5,1);Vertex vertex6 = new Vertex(6,1);

        // ============ topo ============
        g.addVertex(1, Arrays.asList(new Vertex(4,1)));
        g.addVertex(2, Arrays.asList(new Vertex(3,1)));
        g.addVertex(3, Arrays.asList(new Vertex(2,1), new Vertex(4,1), new Vertex(5,1)));
        g.addVertex(4, Arrays.asList(new Vertex(1,1), new Vertex(3,1), new Vertex(5,1)));
        g.addVertex(5, Arrays.asList(new Vertex(3,1), new Vertex(4,1), new Vertex(6,1)));
        g.addVertex(6, Arrays.asList(new Vertex(5,1)));

        List<Integer> path = g.getShortestPath(1, 6);
        int[][] ports = new int[6][6];
        ports[1][4] = 2;
        System.out.println(path);
        System.out.println(path.get(1));
        System.out.println(ports[path.get(0)][path.get(1)]);
    }

}*/


    class Vertex implements Comparable<Vertex> {

        private Integer id;
        private Integer distance;

        public Vertex(Integer id, Integer distance) {
            super();
            this.id = id;
            this.distance = distance;
        }

        public Integer getId() {
            return id;
        }

        public Integer getDistance() {
            return distance;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public void setDistance(Integer distance) {
            this.distance = distance;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((distance == null) ? 0 : distance.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Vertex other = (Vertex) obj;
            if (distance == null) {
                if (other.distance != null)
                    return false;
            } else if (!distance.equals(other.distance))
                return false;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Vertex [id=" + id + ", distance=" + distance + "]";
        }

        @Override
        public int compareTo(Vertex o) {
            if (this.distance < o.distance)
                return -1;
            else if (this.distance > o.distance)
                return 1;
            else
                return this.getId().compareTo(o.getId());
        }

    }

//    class Graph {

    private final Map<Integer, List<Vertex>> vertices;

    public Map<Integer, List<Vertex>> getVertices() {
        return vertices;
    }

    public Dijkstra() {
        this.vertices = new HashMap<Integer, List<Vertex>>();
        this.addVertex(1, Arrays.asList(this.new Vertex(4,1)));  // AP1
        this.addVertex(2, Arrays.asList(this.new Vertex(3,1)));  // AP2
        this.addVertex(3, Arrays.asList(this.new Vertex(2,1), this.new Vertex(4,1), this.new Vertex(5,1)));
        this.addVertex(4, Arrays.asList(this.new Vertex(1,1), this.new Vertex(3,1), this.new Vertex(5,1)));
        this.addVertex(5, Arrays.asList(this.new Vertex(3,1), this.new Vertex(4,1), this.new Vertex(6,1)));
        this.addVertex(6, Arrays.asList(this.new Vertex(5,1))); // AP3
    }

    public void addVertex(Integer integer, List<Vertex> vertex) {
        this.vertices.put(integer, vertex);
    }

    public List<Integer> getShortestPath(Integer src, Integer dst) {
        final Map<Integer, Integer> distances = new HashMap<Integer, Integer>();
        final Map<Integer, Vertex> previous = new HashMap<Integer, Vertex>();
        PriorityQueue<Vertex> nodes = new PriorityQueue<Vertex>();

        for (Integer vertex : vertices.keySet()) {
            if (vertex == src) {
                distances.put(vertex, 0);
                nodes.add(new Vertex(vertex, 0));
            } else {
                distances.put(vertex, Integer.MAX_VALUE);
                nodes.add(new Vertex(vertex, Integer.MAX_VALUE));
            }
            previous.put(vertex, null);
        }

        while (!nodes.isEmpty()) {
            Vertex smallest = nodes.poll();
            if (smallest.getId() == dst) {
                final List<Integer> path = new ArrayList<Integer>();
                while (previous.get(smallest.getId()) != null) {
                    path.add(smallest.getId());
                    smallest = previous.get(smallest.getId());
                }

                path.add(src);
                //System.out.println(path);
                Collections.reverse(path);   // reverse, then return a list from src to dst
                //	System.out.println(path);
                return path;
            }

            if (distances.get(smallest.getId()) == Integer.MAX_VALUE) {
                break;
            }

            for (Vertex neighbor : vertices.get(smallest.getId())) {
                Integer alt = distances.get(smallest.getId()) + neighbor.getDistance();
                if (alt < distances.get(neighbor.getId())) {
                    distances.put(neighbor.getId(), alt);
                    previous.put(neighbor.getId(), smallest);

                    forloop:
                    for (Vertex n : nodes) {
                        if (n.getId() == neighbor.getId()) {
                            nodes.remove(n);
                            n.setDistance(alt);
                            nodes.add(n);
                            break forloop;
                        }
                    }
                }
            }
        }

        List<Integer> list = new ArrayList<Integer>(distances.keySet());
		/*System.out.println(list);
		Collections.reverse(list);
		System.out.println(list);*/
        return list;
    }
//    }
}

