package edu.college.employee.dsa;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;

class TreeGraphTest {
 @Test void graphAndBfsIncludingMissingAndDisconnected(){var g=new EmployeeGraph();for(String x:List.of("a","b","c","z"))g.addVertex(x);g.addEdge("a","b");g.addEdge("a","c");assertEquals(List.of("a","b","c"),g.bfs("a"));assertEquals(List.of(),g.bfs("missing"));assertEquals(List.of("z"),g.bfs("z"));assertThrows(IllegalArgumentException.class,()->g.addEdge("a","missing"));}
 @Test void sampleEmployeeConnectionGraphUsesUndirectedAdjacencyAndBreadthFirstSearch(){
  var employees=List.of(TestEmployee.of(5),TestEmployee.of(3),TestEmployee.of(1),TestEmployee.of(4),TestEmployee.of(2));
  var graph=EmployeeGraph.sampleEmployeeConnections(employees);var adjacency=graph.adjacency();
  assertEquals(List.of("01","02","03","04","05"),List.copyOf(adjacency.keySet()));
  assertEquals(List.of("02","03"),adjacency.get("01"));
  assertEquals(List.of("01","04","05"),adjacency.get("02"));
  assertEquals(List.of("01"),adjacency.get("03"));
  assertEquals(List.of("02"),adjacency.get("04"));
  assertEquals(List.of("02"),adjacency.get("05"));
  assertEquals(List.of("01","02","03","04","05"),graph.bfs("01"));
  assertEquals(List.of(),graph.bfs("missing"));
 }
 @Test void inducedEmployeeGraphShowsOnlySelectedVerticesAndExistingEdges(){
  var graph=new EmployeeGraph();for(String id:List.of("EMP001","EMP002","EMP003","EMP004","EMP005"))graph.addVertex(id);
  graph.addEdge("EMP001","EMP002");graph.addEdge("EMP001","EMP003");graph.addEdge("EMP002","EMP004");graph.addEdge("EMP002","EMP005");
  var selected=graph.inducedSubgraph(new java.util.LinkedHashSet<>(List.of("EMP001","EMP002","EMP005")));
  assertEquals(Map.of("EMP001",List.of("EMP002"),"EMP002",List.of("EMP001","EMP005"),"EMP005",List.of("EMP002")),selected.adjacency());
  assertEquals(List.of("EMP001","EMP002","EMP005"),selected.bfs("EMP001"));
 }
}
