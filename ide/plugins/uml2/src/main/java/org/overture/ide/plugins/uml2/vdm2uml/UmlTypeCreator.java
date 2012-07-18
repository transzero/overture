package org.overture.ide.plugins.uml2.vdm2uml;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.DirectedRelationship;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.RedefinableTemplateSignature;
import org.eclipse.uml2.uml.Relationship;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.TemplateParameter;
import org.eclipse.uml2.uml.TemplateSignature;
import org.eclipse.uml2.uml.TemplateableElement;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.internal.impl.TemplateSignatureImpl;
import org.overture.ast.lex.LexNameToken;
import org.overture.ast.types.ANamedInvariantType;
import org.overture.ast.types.AQuoteType;
import org.overture.ast.types.ASetType;
import org.overture.ast.types.AUnionType;
import org.overture.ast.types.PType;
import org.overture.ast.types.SBasicType;
import org.overture.ast.types.SInvariantType;
import org.overture.ast.types.SNumericBasicType;

public class UmlTypeCreator extends UmlTypeCreatorBase
{
	
	private Model modelWorkingCopy = null;
	private final Map<String, Classifier> types = new HashMap<String, Classifier>();

	public void setModelWorkingCopy(Model modelWorkingCopy)
	{
		this.modelWorkingCopy = modelWorkingCopy;
	}

	// private void addPrimitiveTypes()
	// {
	//
	// types.put("int", modelWorkingCopy.createOwnedPrimitiveType("int"));
	// types.put("bool", modelWorkingCopy.createOwnedPrimitiveType("bool"));
	// types.put("nat", modelWorkingCopy.createOwnedPrimitiveType("nat"));
	// types.put("nat1", modelWorkingCopy.createOwnedPrimitiveType("nat1"));
	// types.put("real", modelWorkingCopy.createOwnedPrimitiveType("real"));
	// types.put("char", modelWorkingCopy.createOwnedPrimitiveType("char"));
	// types.put("token", modelWorkingCopy.createOwnedPrimitiveType("token"));
	// types.put("String", modelWorkingCopy.createOwnedPrimitiveType("String"));
	//
	// }

	public void create(Class class_, LexNameToken name, PType type)
	{
		System.out.println(type + " " + type.kindPType().toString() + " "
				+ getName(type));
		if (types.get(getName(type)) != null)
		{
			return;
		}

		switch (type.kindPType())
		{
			case UNION:
				createNewUmlUnionType(class_, name, (AUnionType) type);
				return;
			case INVARIANT:
				createNewUmlInvariantType(class_, name, (SInvariantType) type);
				return;
			case BASIC:
				convertBasicType(class_, (SBasicType) type);
				return;
			case BRACKET:
				break;
			case CLASS:
				break;
			case FUNCTION:
				break;
			case MAP:
				break;
			case OPERATION:
				break;
			case OPTIONAL:
				break;
			case PARAMETER:
				break;
			case PRODUCT:
				break;
			case QUOTE:
				break;
			case SEQ:
				break;
			case SET:
				createSetType(class_,(ASetType)type);
				return;
			case UNDEFINED:
				break;
			case UNKNOWN:
				break;
			case UNRESOLVED:
				break;
			case VOID:
				break;
			case VOIDRETURN:

			default:
				break;
		}
		if (!types.containsKey(getName(type)))
		{
			Classifier unknownType = modelWorkingCopy.createOwnedPrimitiveType("Unknown");
			unknownType.addKeyword(getName(type));
			types.put(getName(type), unknownType);
		}
	}

	private void createSetType(Class class_, ASetType type)
	{
		if(!types.containsKey(templateSetName))
		{
			Class templateSetClass = modelWorkingCopy.createOwnedClass(templateSetName, false);

			RedefinableTemplateSignature templateT = (RedefinableTemplateSignature) templateSetClass.createOwnedTemplateSignature();
			 templateT.setName("T");
			
//			templateSetClass.setTemplateParameter(t);
			
			types.put(templateSetName, templateSetClass);
		}
	}

	private void createNewUmlUnionType(Class class_, LexNameToken name,
			AUnionType type)
	{

		if (Vdm2UmlUtil.isUnionOfQuotes(type))
		{

			Enumeration enumeration = modelWorkingCopy.createOwnedEnumeration(getName(type));
			for (PType t : type.getTypes())
			{
				if (t instanceof AQuoteType)
				{
					enumeration.createOwnedLiteral(((AQuoteType) t).getValue().value);
				}
			}
			// class_.createNestedClassifier(name.module+"::"+name.name, enumeration.eClass());
			types.put(getName(type), enumeration);
		} else
		{
			// do the constraint XOR

		}

	}

	private void createNewUmlInvariantType(Class class_, LexNameToken name,
			SInvariantType type)
	{
		switch (type.kindSInvariantType())
		{
			case NAMED:
			{
				PType ptype = ((ANamedInvariantType) type).getType();
				create(class_, name, ptype);

				if (!getName(ptype).equals(getName(type)))
				{
					Class recordClass = modelWorkingCopy.createOwnedClass(getName(type), false);

					recordClass.createGeneralization(getUmlType(ptype));
					types.put(getName(type), recordClass);
				}

				break;
			}

			case RECORD:
				break;
		}

	}

	public Classifier getUmlType(PType type)
	{
		String name = getName(type);

		if (types.containsKey(name))
		{
			return types.get(name);
		}
		// else
		// {
		// System.err.println("Trying to find unknown type: "+name);
		return null;
		// }
	}

	private void convertBasicType(Class class_, SBasicType type)
	{
		String typeName = null;
		switch (type.kindSBasicType())
		{
			case BOOLEAN:
				typeName = "bool";
				break;
			case CHAR:
				typeName = "char";
				break;
			case NUMERIC:
				convertNumericType((SNumericBasicType) type);
				return;
			case TOKEN:
				typeName = "token";
				break;
			default:
				assert false : "Should not happen";
				break;
		}

		if (!types.containsKey(getName(type)))
		{
			types.put(getName(type), modelWorkingCopy.createOwnedPrimitiveType(typeName));

		}
	}

	private void convertNumericType(SNumericBasicType type)
	{
		String typeName = null;
		switch (type.kindSNumericBasicType())
		{
			case INT:
				typeName = "int";
				break;
			case NAT:
				typeName = "nat";
				break;
			case NATONE:
				typeName = "nat1";
				break;
			case RATIONAL:
				typeName = "rat";
				break;
			case REAL:
				typeName = "real";
				break;
			default:
				assert false : "Should not happen";
				return;
		}
		if (!types.containsKey(getName(type)))
		{
			types.put(getName(type), modelWorkingCopy.createOwnedPrimitiveType(typeName));

		}
	}
}
