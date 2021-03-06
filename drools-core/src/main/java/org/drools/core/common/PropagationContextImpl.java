/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.common;

import org.drools.core.FactHandle;
import org.drools.core.base.ClassObjectType;
import org.drools.core.marshalling.impl.MarshallerReaderContext;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.WindowTupleList;
import org.drools.core.rule.*;
import org.drools.core.rule.Package;
import org.drools.core.spi.ObjectType;
import org.drools.core.spi.PropagationContext;
import org.drools.core.util.BitMaskUtil;
import org.drools.core.util.ClassUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PropagationContextImpl
        implements
        PropagationContext {

    private static final long               serialVersionUID = 510l;

    private int                             type;

    private Rule                            rule;

    private LeftTuple                       leftTuple;

    private InternalFactHandle              factHandle;

    private long                            propagationNumber;

    private EntryPoint                      entryPoint;
    
    private int                             originOffset;    
    
    private final LinkedList<WorkingMemoryAction> queue1 = new LinkedList<WorkingMemoryAction>(); // for inserts
    
    private LinkedList<WorkingMemoryAction> queue2; // for evaluations and fixers

    private long                            modificationMask = Long.MAX_VALUE;
    private long                            originalMask = Long.MAX_VALUE;

    private Class<?>                        modifiedClass;

    private ObjectType                      objectType;

    // this field is only set for propagations happening during 
    // the deserialization of a session
    private transient MarshallerReaderContext readerContext;

    public PropagationContextImpl() {

    }

    public PropagationContextImpl(final long number,
                                  final int type,
                                  final Rule rule,
                                  final LeftTuple leftTuple,
                                  final InternalFactHandle factHandle) {
        this( number,
              type,
              rule,
              leftTuple,
              factHandle,
              EntryPoint.DEFAULT,
              Long.MAX_VALUE,
              Object.class,
              null );
        this.originOffset = -1;
    }

    public PropagationContextImpl(final long number,
                                  final int type,
                                  final Rule rule,
                                  final LeftTuple leftTuple,
                                  final InternalFactHandle factHandle,
                                  final EntryPoint entryPoint) {
        this( number,
              type,
              rule,
              leftTuple,
              factHandle,
              entryPoint,
              Long.MAX_VALUE,
              Object.class,
              null );
    }

    public PropagationContextImpl(final long number,
                                  final int type,
                                  final Rule rule,
                                  final LeftTuple leftTuple,
                                  final InternalFactHandle factHandle,
                                  final int activeActivations,
                                  final int dormantActivations,
                                  final EntryPoint entryPoint,
                                  final long modificationMask) {
        this( number,
              type,
              rule,
              leftTuple,
              factHandle,
              entryPoint,
              modificationMask,
              Object.class,
              null );
    }

    public PropagationContextImpl(final long number,
                                  final int type,
                                  final Rule rule,
                                  final LeftTuple leftTuple,
                                  final InternalFactHandle factHandle,
                                  final EntryPoint entryPoint,
                                  final MarshallerReaderContext readerContext) {
        this( number,
              type,
              rule,
              leftTuple,
              factHandle,
              entryPoint,
              Long.MAX_VALUE,
              Object.class,
              readerContext );
    }

    public PropagationContextImpl(final long number,
                                  final int type,
                                  final Rule rule,
                                  final LeftTuple leftTuple,
                                  final InternalFactHandle factHandle,
                                  final EntryPoint entryPoint,
                                  final long modificationMask,
                                  final Class<?> modifiedClass,
                                  final MarshallerReaderContext readerContext) {
        this.type = type;
        this.rule = rule;
        this.leftTuple = leftTuple;
        this.factHandle = factHandle;
        this.propagationNumber = number;
        this.entryPoint = entryPoint;
        this.originOffset = -1;
        this.modificationMask = modificationMask;
        this.originalMask = modificationMask;
        this.modifiedClass = modifiedClass;
        this.readerContext = readerContext;
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        this.type = in.readInt();
        this.propagationNumber = in.readLong();
        this.rule = (Rule) in.readObject();
        this.leftTuple = (LeftTuple) in.readObject();
        this.entryPoint = (EntryPoint) in.readObject();
        this.originOffset = in.readInt();
        this.modificationMask = in.readLong();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt( this.type );
        out.writeLong( this.propagationNumber );
        out.writeObject( this.rule );
        out.writeObject( this.leftTuple );
        out.writeObject( this.entryPoint );
        out.writeInt( this.originOffset );
        out.writeLong(this.modificationMask);
    }

    public long getPropagationNumber() {
        return this.propagationNumber;
    }

    public void cleanReaderContext() {
        readerContext = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.kie.reteoo.PropagationContext#getRuleOrigin()
     */
    public Rule getRuleOrigin() {
        return this.rule;
    }

    public org.kie.api.definition.rule.Rule getRule() {
        return this.rule;
    }

    public LeftTuple getLeftTupleOrigin() {
        return this.leftTuple;
    }

    public InternalFactHandle getFactHandleOrigin() {
        return this.factHandle;
    }

    public FactHandle getFactHandle() {
        return this.factHandle;
    }
    
    public void setFactHandle(FactHandle factHandle) {
        this.factHandle = (InternalFactHandle) factHandle;
    }    

    /*
     * (non-Javadoc)
     *
     * @see org.kie.reteoo.PropagationContext#getType()
     */
    public int getType() {
        return this.type;
    }

    public void releaseResources() {
        this.leftTuple = null;
        //this.rule = null;
    }

    /**
     * @return the entryPoint
     */
    public EntryPoint getEntryPoint() {
        return entryPoint;
    }

    /**
     * @param entryPoint the entryPoint to set
     */
    public void setEntryPoint(EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    public void setFactHandle(InternalFactHandle factHandle) {
        this.factHandle = factHandle;
    }

    public int getOriginOffset() {
        return originOffset;
    }

    public void setOriginOffset(int originOffset) {
        this.originOffset = originOffset;
    }

    public void addInsertAction(WorkingMemoryAction action) {
        synchronized (queue1) {
            queue1.addFirst(action);
        }
    }

    public void removeInsertAction(WorkingMemoryAction action) {
        synchronized (queue1) {
            queue1.remove(action);
        }
    }
    
    public LinkedList<WorkingMemoryAction> getQueue1() {
        return this.queue1;
    }

    public LinkedList<WorkingMemoryAction> getQueue2() {
        if ( this.queue2 == null ) {
            this.queue2 = new LinkedList<WorkingMemoryAction>();
        }
        return this.queue2;
    }

    public void evaluateActionQueue(InternalWorkingMemory workingMemory) {
        boolean repeat = true;
        while(repeat) {
            synchronized (queue1) {
                WorkingMemoryAction action;
                while ( (action = (!queue1.isEmpty()) ? queue1.removeFirst() : null ) != null ) {
                    action.execute( workingMemory );
                }
            }

            repeat = false;
            if ( this.queue2 != null ) {
                WorkingMemoryAction action;

                while ( (action = (!queue2.isEmpty()) ? queue2.removeFirst() : null) != null ) {
                    action.execute( workingMemory );
                    if ( !this.queue1.isEmpty() ) {
                        // Queue1 always takes priority and it's contents should be evaluated first
                        repeat = true;
                        break;
                    }
                }
            }

        }
    }

    public long getModificationMask() {
        return modificationMask;
    }

    public PropagationContext adaptModificationMaskForObjectType(ObjectType type, InternalWorkingMemory workingMemory) {
        modificationMask = originalMask;
        if (modificationMask == Long.MAX_VALUE || !(type instanceof ClassObjectType)) {
            return this;
        }

        ClassObjectType classObjectType = (ClassObjectType)type;
        Class<?> classType = classObjectType.getClassType();
        String pkgName = classType.getPackage().getName();

        if (classType == modifiedClass || "java.lang".equals(pkgName) || !(classType.isInterface() || modifiedClass.isInterface())) {
            return this;
        }

        Long cachedMask = classObjectType.getTransformedMask(modifiedClass, originalMask);
        if (cachedMask != null) {
            modificationMask = cachedMask;
            return this;
        }

        modificationMask = 0L;
        List<String> typeClassProps = getSettableProperties(workingMemory, classType, pkgName);
        List<String> modifiedClassProps = getSettableProperties( workingMemory, modifiedClass );

        for (int i = 0; i < modifiedClassProps.size(); i++) {
            if (BitMaskUtil.isPositionSet(originalMask, i)) {
                int posInType = typeClassProps.indexOf(modifiedClassProps.get(i));
                if (posInType >= 0) {
                    modificationMask = BitMaskUtil.set(modificationMask, posInType);
                }
            }
        }
        classObjectType.storeTransformedMask(modifiedClass, originalMask, modificationMask);

        return this;
    }

    private List<String> getSettableProperties(InternalWorkingMemory workingMemory, Class<?> classType) {
        return getSettableProperties(workingMemory, classType, classType.getPackage().getName());
    }

    private List<String> getSettableProperties(InternalWorkingMemory workingMemory, Class<?> classType, String pkgName) {
        return workingMemory.getRuleBase().getPackage(pkgName).getTypeDeclaration(classType).getSettableProperties();
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    public MarshallerReaderContext getReaderContext() {
        return this.readerContext;
    }


    public static String intEnumToString(PropagationContext pctx) {
        String pctxType = null;
        switch( pctx.getType() ) {
            case PropagationContext.INSERTION:
                return "INSERTION";
            case PropagationContext.RULE_ADDITION:
                return "RULE_ADDITION";
            case PropagationContext.MODIFICATION:
                return "MODIFICATION";
            case PropagationContext.RULE_REMOVAL:
                return "RULE_REMOVAL";
            case PropagationContext.DELETION:
                return "DELETION";
            case PropagationContext.EXPIRATION:
                return "EXPIRATION";
        }
        throw new IllegalStateException( "Int type unknown");
    }

    @Override
    public String toString() {
        return "PropagationContextImpl [entryPoint=" + entryPoint + ", factHandle=" + factHandle + ", leftTuple=" + leftTuple + ", originOffset="
               + originOffset + ", propagationNumber=" + propagationNumber + ", rule=" + rule + ", type=" + type + "]";
    }
}
