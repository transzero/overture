package org.overture.ast.definitions.assistants;

import java.util.List;
import java.util.Vector;

import org.overture.ast.definitions.AMutexSyncDefinition;
import org.overture.ast.definitions.PDefinition;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.typechecker.NameScope;

public class AMutexSyncDefinitionAssistantTC {

	public static PDefinition findName(AMutexSyncDefinition d,
			LexNameToken sought, NameScope scope) {
		return null;
	}

	public static List<PDefinition> getDefinitions(AMutexSyncDefinition d) {

		return new Vector<PDefinition>();
	}

	public static LexNameList getVariableNames(AMutexSyncDefinition d) {
		return new LexNameList();
	}

	public static boolean equals(AMutexSyncDefinition def, Object other) {
		
		if (other instanceof AMutexSyncDefinition)
		{
			return def.toString().equals(other.toString());
		}
		
		return false;
	}

}
