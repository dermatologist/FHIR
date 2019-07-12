/**
 * (C) Copyright IBM Corp. 2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.model.type;

import java.util.Collection;
import java.util.Objects;

import javax.annotation.Generated;

import com.ibm.watsonhealth.fhir.model.annotation.Constraint;
import com.ibm.watsonhealth.fhir.model.type.QuantityComparator;
import com.ibm.watsonhealth.fhir.model.util.ValidationSupport;
import com.ibm.watsonhealth.fhir.model.visitor.Visitor;

/**
 * <p>
 * A measured amount (or an amount that can potentially be measured). Note that measured amounts include amounts that are 
 * not precisely quantified, including amounts involving arbitrary units and floating currencies.
 * </p>
 */
@Constraint(
    id = "qty-3",
    level = "Rule",
    location = "(base)",
    description = "If a code for the unit is present, the system SHALL also be present",
    expression = "code.empty() or system.exists()"
)
@Generated("com.ibm.watsonhealth.fhir.tools.CodeGenerator")
public class Quantity extends Element {
    protected final Decimal value;
    protected final QuantityComparator comparator;
    protected final String unit;
    protected final Uri system;
    protected final Code code;

    private volatile int hashCode;

    protected Quantity(Builder builder) {
        super(builder);
        value = builder.value;
        comparator = builder.comparator;
        unit = builder.unit;
        system = builder.system;
        code = builder.code;
        ValidationSupport.requireValueOrChildren(this);
    }

    /**
     * <p>
     * The value of the measured amount. The value includes an implicit precision in the presentation of the value.
     * </p>
     * 
     * @return
     *     An immutable object of type {@link Decimal}.
     */
    public Decimal getValue() {
        return value;
    }

    /**
     * <p>
     * How the value should be understood and represented - whether the actual value is greater or less than the stated value 
     * due to measurement issues; e.g. if the comparator is "&lt;" , then the real value is &lt; stated value.
     * </p>
     * 
     * @return
     *     An immutable object of type {@link QuantityComparator}.
     */
    public QuantityComparator getComparator() {
        return comparator;
    }

    /**
     * <p>
     * A human-readable form of the unit.
     * </p>
     * 
     * @return
     *     An immutable object of type {@link String}.
     */
    public String getUnit() {
        return unit;
    }

    /**
     * <p>
     * The identification of the system that provides the coded form of the unit.
     * </p>
     * 
     * @return
     *     An immutable object of type {@link Uri}.
     */
    public Uri getSystem() {
        return system;
    }

    /**
     * <p>
     * A computer processable form of the unit in some unit representation system.
     * </p>
     * 
     * @return
     *     An immutable object of type {@link Code}.
     */
    public Code getCode() {
        return code;
    }

    @Override
    public boolean hasChildren() {
        return super.hasChildren() || 
            (value != null) || 
            (comparator != null) || 
            (unit != null) || 
            (system != null) || 
            (code != null);
    }

    @Override
    public void accept(java.lang.String elementName, Visitor visitor) {
        if (visitor.preVisit(this)) {
            visitor.visitStart(elementName, this);
            if (visitor.visit(elementName, this)) {
                // visit children
                accept(id, "id", visitor);
                accept(extension, "extension", visitor, Extension.class);
                accept(value, "value", visitor);
                accept(comparator, "comparator", visitor);
                accept(unit, "unit", visitor);
                accept(system, "system", visitor);
                accept(code, "code", visitor);
            }
            visitor.visitEnd(elementName, this);
            visitor.postVisit(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Quantity other = (Quantity) obj;
        return Objects.equals(id, other.id) && 
            Objects.equals(extension, other.extension) && 
            Objects.equals(value, other.value) && 
            Objects.equals(comparator, other.comparator) && 
            Objects.equals(unit, other.unit) && 
            Objects.equals(system, other.system) && 
            Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = Objects.hash(id, 
                extension, 
                value, 
                comparator, 
                unit, 
                system, 
                code);
            hashCode = result;
        }
        return result;
    }

    @Override
    public Builder toBuilder() {
        return new Builder().from(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Element.Builder {
        // optional
        protected Decimal value;
        protected QuantityComparator comparator;
        protected String unit;
        protected Uri system;
        protected Code code;

        protected Builder() {
            super();
        }

        /**
         * <p>
         * Unique id for the element within a resource (for internal references). This may be any string value that does not 
         * contain spaces.
         * </p>
         * 
         * @param id
         *     Unique id for inter-element referencing
         * 
         * @return
         *     A reference to this Builder instance
         */
        @Override
        public Builder id(java.lang.String id) {
            return (Builder) super.id(id);
        }

        /**
         * <p>
         * May be used to represent additional information that is not part of the basic definition of the element. To make the 
         * use of extensions safe and manageable, there is a strict set of governance applied to the definition and use of 
         * extensions. Though any implementer can define an extension, there is a set of requirements that SHALL be met as part 
         * of the definition of the extension.
         * </p>
         * <p>
         * Adds new element(s) to existing list
         * </p>
         * 
         * @param extension
         *     Additional content defined by implementations
         * 
         * @return
         *     A reference to this Builder instance
         */
        @Override
        public Builder extension(Extension... extension) {
            return (Builder) super.extension(extension);
        }

        /**
         * <p>
         * May be used to represent additional information that is not part of the basic definition of the element. To make the 
         * use of extensions safe and manageable, there is a strict set of governance applied to the definition and use of 
         * extensions. Though any implementer can define an extension, there is a set of requirements that SHALL be met as part 
         * of the definition of the extension.
         * </p>
         * <p>
         * Replaces existing list with a new one containing elements from the Collection
         * </p>
         * 
         * @param extension
         *     Additional content defined by implementations
         * 
         * @return
         *     A reference to this Builder instance
         */
        @Override
        public Builder extension(Collection<Extension> extension) {
            return (Builder) super.extension(extension);
        }

        /**
         * <p>
         * The value of the measured amount. The value includes an implicit precision in the presentation of the value.
         * </p>
         * 
         * @param value
         *     Numerical value (with implicit precision)
         * 
         * @return
         *     A reference to this Builder instance
         */
        public Builder value(Decimal value) {
            this.value = value;
            return this;
        }

        /**
         * <p>
         * How the value should be understood and represented - whether the actual value is greater or less than the stated value 
         * due to measurement issues; e.g. if the comparator is "&lt;" , then the real value is &lt; stated value.
         * </p>
         * 
         * @param comparator
         *     &lt; | &lt;= | &gt;= | &gt; - how to understand the value
         * 
         * @return
         *     A reference to this Builder instance
         */
        public Builder comparator(QuantityComparator comparator) {
            this.comparator = comparator;
            return this;
        }

        /**
         * <p>
         * A human-readable form of the unit.
         * </p>
         * 
         * @param unit
         *     Unit representation
         * 
         * @return
         *     A reference to this Builder instance
         */
        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        /**
         * <p>
         * The identification of the system that provides the coded form of the unit.
         * </p>
         * 
         * @param system
         *     System that defines coded unit form
         * 
         * @return
         *     A reference to this Builder instance
         */
        public Builder system(Uri system) {
            this.system = system;
            return this;
        }

        /**
         * <p>
         * A computer processable form of the unit in some unit representation system.
         * </p>
         * 
         * @param code
         *     Coded form of the unit
         * 
         * @return
         *     A reference to this Builder instance
         */
        public Builder code(Code code) {
            this.code = code;
            return this;
        }

        @Override
        public Quantity build() {
            return new Quantity(this);
        }

        private Builder from(Quantity quantity) {
            id = quantity.id;
            extension.addAll(quantity.extension);
            value = quantity.value;
            comparator = quantity.comparator;
            unit = quantity.unit;
            system = quantity.system;
            code = quantity.code;
            return this;
        }
    }
}