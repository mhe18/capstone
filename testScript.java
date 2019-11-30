//This script searches through all instructions that are
//moving a scalar into a register
//and sets an EOL comment in the form "[register] = [value]"
//@category GADC

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileException;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.script.GhidraScript;
import ghidra.app.services.ProgramCoordinator;
import ghidra.framework.model.DomainFile;
import ghidra.framework.model.DomainFolder;
import ghidra.framework.model.DomainObject;
import ghidra.framework.model.Project;
import ghidra.framework.model.ProjectData;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.database.ProgramContentHandler;
import ghidra.program.model.address.Address;
import ghidra.program.model.data.AbstractFloatDataType;
import ghidra.program.model.data.AbstractIntegerDataType;
import ghidra.program.model.data.Array;
import ghidra.program.model.data.BooleanDataType;
import ghidra.program.model.data.DataType;
import ghidra.program.model.data.DataTypeComponent;
import ghidra.program.model.data.Enum;
import ghidra.program.model.data.FunctionDefinition;
import ghidra.program.model.data.GenericCallingConvention;
import ghidra.program.model.data.ParameterDefinition;
import ghidra.program.model.data.Pointer;
import ghidra.program.model.data.Structure;
import ghidra.program.model.data.TypeDef;
import ghidra.program.model.data.Union;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Function.FunctionUpdateType;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Parameter;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.FunctionPrototype;
import ghidra.program.model.pcode.HighConstant;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.HighGlobal;
import ghidra.program.model.pcode.HighLocal;
import ghidra.program.model.pcode.HighOther;
import ghidra.program.model.pcode.HighParam;
import ghidra.program.model.pcode.HighSymbol;
import ghidra.program.model.pcode.HighVariable;
import ghidra.program.model.pcode.PcodeBlock;
import ghidra.program.model.pcode.PcodeBlockBasic;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.PcodeOpAST;
import ghidra.program.model.pcode.SequenceNumber;
import ghidra.program.model.pcode.Varnode;
import ghidra.program.model.pcode.VarnodeAST;
import ghidra.program.model.symbol.ExternalLocation;
import ghidra.program.model.symbol.ExternalManager;
import ghidra.program.model.symbol.Reference;
import ghidra.program.model.symbol.ReferenceManager;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolIterator;
import ghidra.program.model.symbol.SymbolTable;
import ghidra.program.util.CyclomaticComplexity;
import ghidra.util.exception.CancelledException;
import ghidra.util.exception.DuplicateNameException;
import ghidra.util.exception.InvalidInputException;
import ghidra.util.exception.VersionException;
public class testScript extends GhidraScript {

	String SEP = ":";
	String TAB = "\t";
	String QUOTE = "\"";
	
	Set<Function> toProcess = new HashSet<Function>();
        Set<Function> isEP = new HashSet<Function>();
	File outputDirectory;
	PrintWriter ia;
	//HashMap<String, PrintWriter> pws = new HashMap<String, PrintWriter>();
	HashSet<String> decls = new HashSet<String>();
	HashMap<String, String> pairs;
	HashSet<String> types = new HashSet<String>();
	HashMap<HighVariable,VarnodeAST> extraGlobals = new HashMap<HighVariable,VarnodeAST>();
	
	@Override
	protected void run() throws Exception {
		PluginTool tool = state.getTool();
		if (tool == null) {
			println("Script is not running in GUI");
		}
		outputDirectory = askDirectory("Select Directory for Results", "OK");
		try {
			File f = new File(outputDirectory,currentProgram.getName()+".csv");
			ia = new PrintWriter(f);
			//pws.put("input_assist.dl", ia);
			//ia.println("table S2N[string, int]");
			//File f2 = new File(outputDirectory, "S2N.facts");
			//PrintWriter pw = new PrintWriter(f2);
			//pws.put("S2N", pw);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		/*for (Function f : toProcess) {
			//processFunction(ifc, f);
			System.out.println(func.getName() + f.getName());
			if (monitor.isCancelled()) break;
		}*/
		Project project = state.getProject();
		String[] libraryNames  = this.currentProgram.getExternalManager().getExternalLibraryNames();
		ProjectData projectData = project.getProjectData();
		DomainFolder folder = projectData.getRootFolder();
		for (String library : libraryNames) {
			
			 DomainFile file = folder.getFile(library);
			 if(file!=null) {
			 processDomainFiles(file);
			 }
		}
		
		Map<Function, Set<Function>> graph = new HashMap<>();
		for (Function f : this.currentProgram.getFunctionManager().getFunctions(true)) {
			
			graph.put(f, f.getCalledFunctions(monitor));
			//System.out.println(f.getEntryPoint());
			//System.out.println(f.getName() + f.isExternal());
		}
		
		ia.println("function_name;complexity;function_prototype;function_size;Parameters;entry_point;function_call_entry_point");
		
		for (Function f : graph.keySet()) {
			Set<Function> neighbors = graph.get(f);
			SymbolIterator externalSymbols = f.getProgram().getSymbolTable().getSymbols(f.getName());
			while (externalSymbols.hasNext()) {
			    Symbol next = externalSymbols.next();
			  //  System.out.println(next.getName());
			    }
			CyclomaticComplexity cyclomaticComplexity = new CyclomaticComplexity();
			ArrayList<Address> call_func = new ArrayList<Address>();
			if (neighbors != null) {
				Iterator<Function> it = neighbors.iterator();
				while(it.hasNext()) {
					Function n = it.next();
					if(f.getName().equals(n.getName())) {
						it.remove();
					}else {
						call_func.add(n.getEntryPoint());
					}
					
				}
			}
			ia.println(f.getName() + ";" + cyclomaticComplexity.calculateCyclomaticComplexity(f, monitor) + ";" +  f.getPrototypeString(true, true) +";" +  f.getBody().getNumAddresses() + ";" + Arrays.toString(f.getParameters()) + ";" + f.getEntryPoint() + ";" + call_func);
		}
	
		
		

		
		closeFiles();
	}


	private void processDomainFiles(DomainFile file) {
		// TODO Auto-generated method stub
		 if (ProgramContentHandler.PROGRAM_CONTENT_TYPE == file.getContentType()) {
			 return;
		 }
		 if(file.isVersioned() && !file.isCheckedOut()) {
			 System.out.println("WARNING! Skipping versioned file - not checked-out: " + file.getPathname()); 
		 }
		 try {
			Program domainObject = (Program) file.getDomainObject(this, false, false, monitor);
			processProgram(domainObject);
		} catch (VersionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CancelledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}


	private void processProgram(Program program) {
		String externalLibraryName = program.getDomainFile().getName();
		
		monitor.setMessage("Processing: " + externalLibraryName);
		        

		FunctionManager newFunctionManager = program.getFunctionManager();
		FunctionIterator newFunctions = newFunctionManager.getFunctions(true);
		ExternalManager currentExternalManager = this.currentProgram.getExternalManager();
		int newFunctionCount = newFunctionManager.getFunctionCount();
		monitor.setMaximum(newFunctionManager.getFunctionCount());
		while (newFunctions.hasNext()) {
			 try {
				monitor.checkCanceled();
				monitor.incrementProgress(1);
				Function current_function = newFunctions.next();
		        SymbolIterator currentAllSymbols = currentProgram.getSymbolTable().getAllSymbols(true);
		        while(currentAllSymbols.hasNext()) {
		        	Symbol current_symbol = currentAllSymbols.next();
		        	ExternalLocation externalLocation;
					if (current_symbol.getAddress() == current_function.getEntryPoint()) {
		        		try {
							externalLocation = currentExternalManager.addExtFunction(externalLibraryName,
									current_function.getName(),
									current_symbol.getAddress(),
							        SourceType.USER_DEFINED);
							Function externalFunction = externalLocation.getFunction();
							 String newCallingConvention = current_function.getCallingConventionName();
			                    Parameter newReturnValue = current_function.getReturn();
			                    FunctionUpdateType newUpdateType = Function.FunctionUpdateType.DYNAMIC_STORAGE_FORMAL_PARAMS;
			                    SourceType newSource = SourceType.USER_DEFINED;
			                    Parameter[] newParameters = current_function.getParameters();
			                    externalFunction.updateFunction(newCallingConvention, newReturnValue,
		                                newUpdateType, true, newSource, newParameters);
			                    ReferenceManager currentReferenceManager = currentProgram.getReferenceManager();
								for (Reference reference: currentReferenceManager.getReferencesTo(current_symbol.getAddress())) {
									currentReferenceManager.addExternalReference(reference.getFromAddress(),
                                            externalLibraryName,
                                            current_function.getName(),
                                            current_function.getEntryPoint(),
                                            SourceType.USER_DEFINED,
                                            reference.getOperandIndex(),
                                            reference.getReferenceType());
			                    }
			                        

						} catch (DuplicateNameException | InvalidInputException  e ) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	        
					}

		        }

			} catch (CancelledException | NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	         
		}





		
	}


	
	private void closeFiles() {
		//for (PrintWriter pw : pws.values()) {
		//	pw.flush();
		//	pw.close();
		//}
		ia.close();
	}
	
	
}
