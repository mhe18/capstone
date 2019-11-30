package test2Neo4j;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException; 
import java.util.Scanner; 
import java.util.*;  

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.Label;
import org.neo4j.io.fs.FileUtils;

//import org.neo4j.graphdb.traversal;

public class test2 {

  private enum RelTypes implements RelationshipType{
	  CALLS, ExternalCall
  }
  
  public static Node findNode(ArrayList<Node> list, String entry_point) {
	  for (Node function:list) {
		  if(function.getProperty("entry_point").equals(entry_point)) {
			  return function;
		  }
	  }
	  return null;
  }
  
  public static void main(String[] args) throws FileNotFoundException {
	String DB_PATH = "C:\\Program Files\\neo4j-community-3.5.12\\data\\databases\\graph.db";
	File database = new File(DB_PATH);
	GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
	GraphDatabaseService db= dbFactory.newEmbeddedDatabase(database);
	ArrayList<Node> functions = new ArrayList<Node>();
	ArrayList<Relationship> calls = new ArrayList<Relationship>();
	//File file = new File("C:\\workspace\\test2Neo4j\\src\\test2Neo4j\\input2.csv"); 
	File file = new File("C:\\workspace\\test2Neo4j\\src\\test2Neo4j\\adventurev5_stripped.csv"); 
	Scanner sc = new Scanner(file);
	String header = sc.next();
	sc.useDelimiter("\\n"); 
	try (Transaction tx = db.beginTx()){
		db.execute("MATCH (n)\r\nDETACH DELETE n\r\n");
		while(sc.hasNext()) {
			String line = sc.next().replace("\n", "").replace("\r", "");
			String[] columns = line.split(";",5);
			Node newFunc = findNode(functions, columns[3]);
			if (newFunc==null) {
				newFunc = db.createNode();
				newFunc.setProperty("entry_point", columns[3]);
				functions.add(newFunc);
			}
			newFunc.setProperty("name", columns[0]);
			//System.out.println(columns[1]);
			int complexity = Integer.parseInt(columns[1]);
			newFunc.setProperty("complexity", complexity);
			newFunc.setProperty("pagerank", (complexity+10)*2);			
			String parameterString = columns[2].substring(1, columns[2].length() - 1);
			if (parameterString.length() != 0) {
				//String[] parameters = parameterString.split(", ",-1);
				String parameters = parameterString.replace("[", "").replace("]", "");
				newFunc.setProperty("parameters", parameters);
			}
			else {
				newFunc.setProperty("parameters", "");
			}
			String calleeString = columns[4].substring(1, columns[4].length() - 1);
			if (calleeString.length() != 0) {
				String[] callees = calleeString.split(", ",-1);
				for(String callee: callees) {
					Node calleeFunc = findNode(functions, callee);
					if (calleeFunc == null) {
						calleeFunc = db.createNode();
						calleeFunc.setProperty("entry_point", callee);
						functions.add(calleeFunc);
					}
					Relationship newRel = newFunc.createRelationshipTo(calleeFunc, RelTypes.CALLS);
					calls.add(newRel);	
				}
			}			
			if(columns[0].contains("FUN_")) {
				newFunc.addLabel(Label.label("Function"));
				newFunc.setProperty("community", "Function");
			}
			else {
				newFunc.addLabel(Label.label("LibFunction"));
				newFunc.setProperty("community", "LibFunction");
			}
		}
		tx.success();
	}
	db.shutdown();
	System.out.println("Done successfully");
	}
  
}





