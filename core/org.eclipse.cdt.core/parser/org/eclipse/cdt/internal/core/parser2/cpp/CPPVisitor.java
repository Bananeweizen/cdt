/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Nov 29, 2004
 */
package org.eclipse.cdt.internal.core.parser2.cpp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.c.ICFunctionScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDirective;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespace;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPScope;
import org.eclipse.cdt.core.parser.util.CharArrayUtils;
import org.eclipse.cdt.core.parser.util.ObjectMap;
import org.eclipse.cdt.core.parser.util.ObjectSet;
import org.eclipse.cdt.internal.core.parser2.c.CASTFunctionDeclarator;

/**
 * @author aniefer
 */
public class CPPVisitor {

	/**
	 * @param name
	 */
	public static IBinding createBinding(IASTName name) {
		IASTNode parent = name.getParent();
		if( parent instanceof IASTNamedTypeSpecifier  ||
		    parent instanceof ICPPASTQualifiedName  ) 
		{
			return resolveBinding( name );
		} else if( parent instanceof IASTIdExpression ){
			return resolveBinding( parent );
		} else if( parent instanceof ICPPASTCompositeTypeSpecifier ){
			return createBinding( (ICPPASTCompositeTypeSpecifier) parent );
		} else if( parent instanceof IASTDeclarator ){
			return createBinding( (IASTDeclarator) parent );
		} else if( parent instanceof ICPPASTElaboratedTypeSpecifier ){
			return createBinding( (ICPPASTElaboratedTypeSpecifier) parent );
		}  
		return null;
	}
	
	private static IBinding createBinding( ICPPASTElaboratedTypeSpecifier elabType ){
		ICPPScope scope = (ICPPScope) getContainingScope( elabType );
		CPPClassType binding = (CPPClassType) scope.getBinding( 0, elabType.getName().toCharArray() );
		if( binding == null ){
			if( elabType.getKind() != IASTElaboratedTypeSpecifier.k_enum )
				binding = new CPPClassType( elabType );
			scope.addBinding( binding );
		} else {
			binding.addDeclaration( elabType );
		}
		return binding;
	}
	private static IBinding createBinding( ICPPASTCompositeTypeSpecifier compType ){
		ICPPScope scope = (ICPPScope) getContainingScope( compType );
		CPPClassType binding = (CPPClassType) scope.getBinding( 0, compType.getName().toCharArray() );
		if( binding == null ){
			binding = new CPPClassType( compType );
			scope.addBinding( binding );
		} else {
			binding.addDefinition( compType );
		}
		return binding;
	}
	
	private static IBinding createBinding( IASTDeclarator declarator ){
		IBinding binding = null;
		IASTNode parent = declarator.getParent();
		if( declarator instanceof ICPPASTFunctionDeclarator ){
			IScope scope = getContainingScope( parent );
			if( scope instanceof ICPPClassScope )
				binding = new CPPMethod( (ICPPASTFunctionDeclarator) declarator );
			else
				binding = new CPPFunction( (ICPPASTFunctionDeclarator) declarator );
		} else {
			if( parent instanceof IASTSimpleDeclaration ){
				IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) parent;
				if( simpleDecl.getParent() instanceof ICPPASTCompositeTypeSpecifier ){
					binding = new CPPField( declarator );
				} else {
					binding = new CPPVariable( declarator );
				}
			} else if( parent instanceof IASTParameterDeclaration ){
				
			} else if( parent instanceof IASTFunctionDefinition ){
				
			}
		}
		return binding;
	}

	public static IScope getContainingScope( IASTNode node ){
		if( node instanceof IASTName )
			return getContainingScope( (IASTName) node );
		else if( node instanceof IASTDeclaration )
	        return getContainingScope( (IASTDeclaration) node );
	    else if( node instanceof IASTStatement )
	        return getContainingScope( (IASTStatement) node );
	    else if( node instanceof IASTDeclSpecifier )
	        return getContainingScope( (IASTDeclSpecifier) node );
	    else if( node instanceof IASTParameterDeclaration )
	        return getContainingScope( (IASTParameterDeclaration) node );
	    else if( node instanceof IASTEnumerator ){
	        //put the enumerators in the same scope as the enumeration
	        return getContainingScope( (IASTEnumerationSpecifier) node.getParent() );
	    }
	    
	    return getContainingScope( node.getParent() );
	}
	
	public static IScope getContainingScope( IASTName name ){
		IASTNode parent = name.getParent();
		if( parent instanceof ICPPASTQualifiedName ){
			IASTName [] names = ((ICPPASTQualifiedName) parent).getNames();
			int i = 0;
			for( ; i < names.length; i++ ){
				if( names[i] == name ) break;
			}
			if( i > 0 ){
				IBinding binding = names[i - 1].resolveBinding();
				if( binding instanceof ICPPClassType ){
					return ((ICPPClassType)binding).getCompositeScope();
				} else if( binding instanceof ICPPNamespace ){
					return ((ICPPNamespace)binding).getNamespaceScope();
				}
			}
		} 
		return getContainingScope( parent );
	}
	/**
	 * @param declaration
	 * @return
	 */
	public static IScope getContainingScope(IASTDeclaration declaration) {
		IASTNode parent = declaration.getParent();
		if( parent instanceof IASTTranslationUnit ){
			return ((IASTTranslationUnit)parent).getScope();
		} else if( parent instanceof IASTDeclarationStatement ){
			return getContainingScope( (IASTStatement) parent );
		} else if( parent instanceof IASTForStatement ){
		    return ((IASTForStatement)parent).getScope();
		} else if( parent instanceof IASTCompositeTypeSpecifier ){
		    return ((IASTCompositeTypeSpecifier)parent).getScope();
		}
		
		return null;
	}
	
	public static IScope getContainingScope( IASTStatement statement ){
		IASTNode parent = statement.getParent();
		IScope scope = null;
		if( parent instanceof IASTCompoundStatement ){
		    IASTCompoundStatement compound = (IASTCompoundStatement) parent;
		    scope = compound.getScope();
		} else if( parent instanceof IASTStatement ){
			scope = getContainingScope( (IASTStatement)parent );
		} else if( parent instanceof IASTFunctionDefinition ){
			IASTFunctionDeclarator fnDeclarator = ((IASTFunctionDefinition) parent ).getDeclarator();
			IFunction function = (IFunction) fnDeclarator.getName().resolveBinding();
			scope = function.getFunctionScope();
		}
		
		if( statement instanceof IASTGotoStatement || statement instanceof IASTLabelStatement ){
		    //labels have function scope
		    while( scope != null && !(scope instanceof ICFunctionScope) ){
		        scope = scope.getParent();
		    }
		}
		
		return scope;
	}
	
	public static IScope getContainingScope( IASTDeclSpecifier compTypeSpec ){
	    IASTNode parent = compTypeSpec.getParent();
	    return getContainingScope( (IASTSimpleDeclaration) parent );
	}

	/**
	 * @param parameterDeclaration
	 * @return
	 */
	public static IScope getContainingScope(IASTParameterDeclaration parameterDeclaration) {
		IASTNode parent = parameterDeclaration.getParent();
		if( parent instanceof IASTFunctionDeclarator ){
			IASTFunctionDeclarator functionDeclarator = (IASTFunctionDeclarator) parent;
			IASTName fnName = functionDeclarator.getName();
			IFunction function = (IFunction) fnName.resolveBinding();
			return function.getFunctionScope();
		}
		
		return null;
	}
	
	private static IASTNode getContainingBlockItem( IASTNode node ){
		IASTNode parent = node.getParent();
		if( parent == null )
			return null;
		
		if( parent instanceof IASTDeclaration ){
			IASTNode p = parent.getParent();
			if( p instanceof IASTDeclarationStatement )
				return p;
			return parent;
		}
		//if parent is something that can contain a declaration
		else if ( parent instanceof IASTCompoundStatement || 
				  parent instanceof IASTTranslationUnit   ||
				  parent instanceof IASTForStatement )
		{
			return node;
		}
		
		return getContainingBlockItem( parent );
	}
	
	static private IBinding resolveBinding( IASTNode node ){
		if( node instanceof IASTIdExpression ){
			return resolveBinding( ((IASTIdExpression)node).getName() );
		}
		return null;
	}
	
	static protected class LookupData
	{
		public char[] name;
		public ObjectMap usingDirectives = ObjectMap.EMPTY_MAP; 
		public ObjectSet visited = ObjectSet.EMPTY_SET;	//used to ensure we don't visit things more than once
		public ObjectSet inheritanceChain;	//used to detect circular inheritance
		
		public boolean qualified = false;
		public boolean ignoreUsingDirectives = false;
		public boolean usingDirectivesOnly = false;
		public boolean forDefinition = false;
		
		public List foundItems = null;
		
		public LookupData( char[] n ){
			name = n;
		}
	}
	
	static private IBinding resolveBinding( IASTName name ){
		//1: get some context info off of the name to figure out what kind of lookup we want
		LookupData data = createLookupData( name );
		
		//2: lookup
		lookup( data, name );
		
		//3: resolve ambiguities
		//TODO
		if( data.foundItems != null && data.foundItems.size() == 1 ){
			IASTName found = (IASTName) data.foundItems.get(0);
			IBinding binding = found.resolveBinding();
			if( data.forDefinition ){
				addDefinition( binding, name );
			}
			return binding;
		}
		return null;
	}
	private static void addDefinition( IBinding binding, IASTName name ){
		if( binding instanceof IFunction ){
			IASTNode node =  name.getParent();
			if( node instanceof ICPPASTQualifiedName )
				node = node.getParent();
			if( node instanceof IASTFunctionDeclarator ){
				((CPPFunction)binding).addDefinition( (IASTFunctionDeclarator) node );
			}
		}
	}
	static private LookupData createLookupData( IASTName name ){
		LookupData data = new LookupData( name.toCharArray() );
		IASTNode parent = name.getParent();
		if( parent instanceof ICPPASTQualifiedName ){
			data.qualified = true;
			parent = parent.getParent();
			if( parent instanceof IASTDeclarator ){
				data.forDefinition = true;
			}
		} else if( parent instanceof IASTDeclarator ){
			data.forDefinition = true;
		}
		return data;
	}
	
	static private IASTName collectResult( LookupData data, IASTNode declaration, boolean checkAux ){
		if( declaration instanceof IASTDeclarationStatement )
			declaration = ((IASTDeclarationStatement)declaration).getDeclaration();
		else if( declaration instanceof IASTForStatement )
			declaration = ((IASTForStatement)declaration).getInitDeclaration();
		
		if( declaration == null )
			return null;
		
		if( declaration instanceof IASTSimpleDeclaration ){
			IASTSimpleDeclaration simpleDeclaration = (IASTSimpleDeclaration) declaration;
			IASTDeclarator [] declarators = simpleDeclaration.getDeclarators();
			for( int i = 0; i < declarators.length; i++ ){
				IASTDeclarator declarator = declarators[i];
				IASTName declaratorName = declarator.getName();
				if( CharArrayUtils.equals( declaratorName.toCharArray(), data.name ) ){
					return declaratorName;
				}
			}

			//decl spec 
			IASTDeclSpecifier declSpec = simpleDeclaration.getDeclSpecifier();
			if( declSpec instanceof IASTElaboratedTypeSpecifier ){
				IASTName elabName = ((IASTElaboratedTypeSpecifier)declSpec).getName();
				if( CharArrayUtils.equals( elabName.toCharArray(), data.name ) ){
					return elabName;
				}
			} else if( declSpec instanceof ICPPASTCompositeTypeSpecifier ){
				IASTName compName = ((IASTCompositeTypeSpecifier)declSpec).getName();
				if( CharArrayUtils.equals( compName.toCharArray(), data.name ) ){
					return compName;
				}
			} else if( declSpec instanceof IASTEnumerationSpecifier ){
			    IASTEnumerationSpecifier enumeration = (IASTEnumerationSpecifier) declSpec;
			    IASTName eName = enumeration.getName();
			    if( CharArrayUtils.equals( eName.toCharArray(), data.name ) ){
					return eName;
				}
			    //check enumerators too
			    IASTEnumerator [] list = enumeration.getEnumerators();
			    for( int i = 0; i < list.length; i++ ) {
			        IASTEnumerator enumerator = list[i];
			        if( enumerator == null ) break;
			        eName = enumerator.getName();
			        if( CharArrayUtils.equals( eName.toCharArray(), data.name ) ){
						return eName;
					}
			    }
			    
			}
		} else if( declaration instanceof IASTFunctionDefinition ){
			IASTFunctionDefinition functionDef = (IASTFunctionDefinition) declaration;
			CASTFunctionDeclarator declarator = (CASTFunctionDeclarator) functionDef.getDeclarator();
			
			//check the function itself
			IASTName declName = declarator.getName();
			if( CharArrayUtils.equals( declName.toCharArray(), data.name ) ){
				return declName;
			}
			if( checkAux ) {
				//check the parameters
				IASTParameterDeclaration []  parameters = declarator.getParameters();
				for( int i = 0; i < parameters.length; i++ ){
					IASTParameterDeclaration parameterDeclaration = parameters[i];
					if( parameterDeclaration == null ) break;
					declName = parameterDeclaration.getDeclarator().getName();
					if( CharArrayUtils.equals( declName.toCharArray(), data.name ) ){
						return declName;
					}
				}
			}
		}
		return null;
	}
	static private void lookup( LookupData data, IASTName name ){
		IASTNode node = name; 
		
		ICPPScope scope = (ICPPScope) getContainingScope( node );		
		while( scope != null ){
			IASTNode blockItem = getContainingBlockItem( node );
			List directives = null;
			if( !data.usingDirectivesOnly )
				directives = lookupInScope( data, scope, blockItem );
			
			if( !data.ignoreUsingDirectives ) {
				data.visited.clear();
				if( data.foundItems == null || data.foundItems.isEmpty() ){
					List transitives = lookupInNominated( data, scope, null );
					
					processDirectives( data, scope, transitives );
					if( directives != null && directives.size() != 0 )
						processDirectives( data, scope, directives );
					
					while( !data.usingDirectives.isEmpty() && data.usingDirectives.get( scope ) != null ){
						transitives = lookupInNominated( data, scope, null );
		
						if( !data.qualified || data.foundItems == null ){
							processDirectives( data, scope, transitives );
						}
					}
				}
			}
			
			if( data.foundItems != null && !data.foundItems.isEmpty() )
				return;
			
			if( !data.usingDirectivesOnly && scope instanceof ICPPClassScope ){
				//TODO : lookupInParents
			}
			
			if( data.foundItems != null && !data.foundItems.isEmpty() )
				return;
			
			//if still not found, loop and check our containing scope
			if( data.qualified && !data.usingDirectives.isEmpty() )
				data.usingDirectivesOnly = true;
			node = blockItem.getParent();
			scope = (ICPPScope) scope.getParent();
		}
	}
	
	static private void processDirectives( LookupData data, IScope scope, List directives ){
		if( directives == null || directives.size() == 0 )
			return;
		
		ICPPScope enclosing = null;
		IScope temp = null;
		
		int size = directives.size();
		for( int i = 0; i < size; i++ ){
			IASTName qualName = ((ICPPASTUsingDirective)directives.get(1)).getQualifiedName();
			IBinding binding = qualName.resolveBinding();
			if( binding instanceof ICPPNamespaceScope ){
				temp = (IScope) binding;
			} else
				continue;
				
			//namespace are searched at most once
			if( !data.visited.containsKey( temp ) ){
				enclosing = getClosestEnclosingScope( scope, temp );
				
				//data.usingDirectives is a map from enclosing scope to a list
				//of namespaces to consider when we reach that enclosing scope
				ICPPScope [] list = data.usingDirectives.isEmpty() ? null : (ICPPScope []) data.usingDirectives.get( enclosing );
				if( list == null ){
					list = new ICPPScope [] { enclosing, null };
					if( data.usingDirectives == ObjectMap.EMPTY_MAP ){
						data.usingDirectives = new ObjectMap(2);
					}
					data.usingDirectives.put( enclosing, list );
				} else {
					int j = 0;
					for( ; j < list.length; j++ ){
						if( list[j] == null ){
							list[j] = enclosing;
						}
					}
					if( j == list.length ){
						ICPPScope [] tmpScopes = new ICPPScope[ list.length * 2 ];
						System.arraycopy( list, 0, tmpScopes, 0, list.length );
						tmpScopes[list.length] = enclosing;
						list = tmpScopes;
					}
				}
			}
		}
		
	}
	static private ICPPScope getClosestEnclosingScope( IScope scope1, IScope scope2 ){
		return null;
	}
	/**
	 * 
	 * @param scope
	 * @return List of encountered using directives
	 */
	static private List lookupInScope( LookupData data, ICPPScope scope, IASTNode blockItem ) {
		IASTName possible = null;
		IASTNode [] nodes = null;
		IASTNode parent = scope.getPhysicalNode();

		List usingDirectives = null;
		
		if( parent instanceof IASTCompoundStatement ){
			IASTCompoundStatement compound = (IASTCompoundStatement) parent;
			nodes = compound.getStatements();
		} else if ( parent instanceof IASTTranslationUnit ){
			IASTTranslationUnit translation = (IASTTranslationUnit) parent;
			nodes = translation.getDeclarations();
		} else if ( parent instanceof ICPPASTCompositeTypeSpecifier ){
			ICPPASTCompositeTypeSpecifier comp = (ICPPASTCompositeTypeSpecifier) parent;
			nodes = comp.getMembers();
		}

		int idx = -1;
		IASTNode item = ( nodes != null ? (nodes.length > 0 ? nodes[++idx] : null ) : parent );

		while( item != null ) {
			if( item == null || item == blockItem )
				break;
			
			if( item instanceof ICPPASTUsingDirective && !data.ignoreUsingDirectives ) {
				if( usingDirectives == null )
					usingDirectives = new ArrayList(2);
				usingDirectives.add( item );
				continue;
			}
			possible = collectResult( data, item, (item == parent)  );
			if( possible != null ){
				if( data.foundItems == null )
					data.foundItems = new ArrayList(2);
				data.foundItems.add( possible );
			}
			if( idx > -1 && ++idx < nodes.length ){
				item = nodes[idx];
			} else {
				item = null;
			}
		}
		return usingDirectives;
	}
	
	static private List lookupInNominated( LookupData data, ICPPScope scope, List transitives ){
		if( data.usingDirectives.isEmpty() )
			return transitives;
		
		List directives = null;
		ICPPScope temp = null;
		
		directives = (List) data.usingDirectives.remove( scope );
		if( directives == null || directives.size() == 0 ) {
			return transitives;
		}
		for( int i = 0; i < directives.size(); i++ ){
			temp = (ICPPScope) directives.get(i);
			if( !data.visited.containsKey( temp ) ){
				if( data.visited == ObjectSet.EMPTY_SET ) {
					data.visited = new ObjectSet(2);
				}
				data.visited.put( temp );
				int pre = ( data.foundItems != null ) ? 0 : data.foundItems.size();
				List usings = lookupInScope( data, scope, null );
				int post = ( data.foundItems != null ) ? 0 : data.foundItems.size();
				
				//only consider the transitive using directives if we are an unqualified
				//lookup, or we didn't find the name in decl
				if( usings != null && usings.size() > 0 && (!data.qualified || (pre == post)) ){
					transitives.addAll( usings );
				}
			}
		}
		return transitives;
	}
}
