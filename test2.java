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
	  CALLS, ExternalCall, CONTAINS
  }
  
  public static Node findNode(ArrayList<Node> list, String property, String value) {
	  //check which node contains property with value
	  //assume that only one item in the list matches the value
	  //return the (first) one
	  for (Node item:list) {
		  if(item.getProperty(property).equals(value)) {
			  return item;
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
	ArrayList<Node> files = new ArrayList<Node>();
	ArrayList<Relationship> contains = new ArrayList<Relationship>();
	int fileID = 0;
	Node tempFile;
	//File file = new File("C:\\workspace\\test2Neo4j\\src\\test2Neo4j\\input2.csv"); 
	//File file = new File("C:\\workspace\\test2Neo4j\\src\\test2Neo4j\\adventurev5_stripped.csv"); 
	//File file = new File("C:\\workspace\\test2Neo4j\\src\\test2Neo4j\\libtinfo.so.5.csv"); 
	File file = new File("C:\\workspace\\test2Neo4j\\src\\test2Neo4j\\1.exe.csv"); 
	String fileName = "libtinfo.so.5";
	Scanner sc = new Scanner(file);
	String header = sc.next();
	sc.useDelimiter("\\n"); 
	try (Transaction tx = db.beginTx()){
		db.execute("MATCH (n)\r\nDETACH DELETE n\r\n");
		while(sc.hasNext()) {
			String line = sc.next().replace("\n", "").replace("\r", "");
			String[] columns = line.split(";",8);
			Node newFunc = findNode(functions, "name", columns[0]);
			if (newFunc==null) {
				newFunc = db.createNode();
				newFunc.setProperty("name", columns[0]);
				functions.add(newFunc);
			}
			int complexity = Integer.parseInt(columns[1]);
			newFunc.setProperty("complexity", complexity);
			newFunc.setProperty("pagerank", (complexity+10)*2);	
			newFunc.setProperty("prototype", columns[2]);
			newFunc.setProperty("size", columns[3]);
			if(newFunc.hasProperty("entry_point")) {
				Object prevEntryPoint = newFunc.getProperty("entry_point");
				newFunc.setProperty("entry_point", prevEntryPoint+", "+columns[7]);
			}
			else {
				newFunc.setProperty("entry_point", columns[5]);
				
			}
			newFunc.setProperty("entry_point", columns[5]);
			newFunc.setProperty("fileName", columns[7]);
			//distinguish lib func
			if (columns[7].equals(fileName)) {
				newFunc.setProperty("isLib", 0);
			}
			else {
				newFunc.setProperty("isLib", 1);
			}
			//distinguish between files
			int id = 0;
			int num = 0;
			tempFile = findNode(files, "name", columns[7]);
			if(tempFile != null) {
				id = (int) tempFile.getProperty("id");
				num = (int)tempFile.getProperty("funcNum");
				tempFile.setProperty("funcNum", num+1);
			}
			else {
				tempFile = db.createNode();
				tempFile.setProperty("id", fileID);
				fileID ++;
				tempFile.setProperty("name", columns[7]);
				tempFile.setProperty("funcNum", 1);
				tempFile.addLabel(Label.label("File"));
				files.add(tempFile);
			}
			Relationship fileContains = tempFile.createRelationshipTo(newFunc, RelTypes.CONTAINS);
			contains.add(fileContains);
			newFunc.setProperty("fileID", tempFile.getProperty("id"));
			
			String parameterString = columns[4].substring(1, columns[4].length() - 1);
			if (parameterString.length() != 0) {
				String parameters = parameterString.replace("[", "").replace("]", "");
				newFunc.setProperty("parameters", parameters);
			}
			else {
				newFunc.setProperty("parameters", "");
			}			
			String calleeString = columns[6].substring(1, columns[6].length() - 1);
			if (calleeString.length() != 0) {
				String[] callees = calleeString.split(", ",-1);
				for(String callee: callees) {
					Node calleeFunc = findNode(functions, "name", callee);
					if (calleeFunc == null) {
						calleeFunc = db.createNode();
						calleeFunc.setProperty("name", callee);
						functions.add(calleeFunc);
					}
					Relationship newRel = newFunc.createRelationshipTo(calleeFunc, RelTypes.CALLS);
					calls.add(newRel);	
				}
			}			
			newFunc.addLabel(Label.label("Function"));
		}
		tx.success();
	}
	db.shutdown();
	System.out.println("Done successfully");
	}
  
}





