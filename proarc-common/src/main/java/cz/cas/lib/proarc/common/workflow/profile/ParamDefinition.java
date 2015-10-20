/*
 * Copyright (C) 2015 Jan Pokorsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.common.workflow.profile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlValue;

/**
 *
 * @author Jan Pokorsky
 */
@XmlAccessorType(value = XmlAccessType.FIELD)
public class ParamDefinition {

    @XmlAttribute(name = WorkflowProfileConsts.PARAM_TYPE_ATT, required = true)
    @XmlIDREF
    private ParamTypeDefinition type;

    @XmlValue
    private String value;

    public ParamTypeDefinition getType() {
        return type;
    }

    public ParamDefinition setType(ParamTypeDefinition type) {
        this.type = type;
        return this;
    }

    public String getValue() {
        return value;
    }

    public ParamDefinition setValue(String value) {
        this.value = value;
        return this;
    }

}
