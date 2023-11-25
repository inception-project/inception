/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.support;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * user-defined enum "types"
 * 
 * @param <T>
 *            the enum type
 *
 */
public abstract class PersistentEnumIntegerUserType<T extends Enum<T>>
    implements UserType<T>, Serializable
{
    private static final long serialVersionUID = -3080625439869047088L;

    @Override
    public T assemble(Serializable cached, Object owner) throws HibernateException
    {
        return (T) cached;
    }

    @Override
    public T deepCopy(T value) throws HibernateException
    {
        return value;
    }

    @Override
    public Serializable disassemble(T value) throws HibernateException
    {
        return (Serializable) value;
    }

    @Override
    public boolean equals(T x, T y) throws HibernateException
    {
        return x == y;
    }

    @Override
    public int hashCode(T x) throws HibernateException
    {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public T nullSafeGet(ResultSet rs, int aPosition, SharedSessionContractImplementor session,
            Object owner)
        throws HibernateException, SQLException
    {
        int dbValue = rs.getInt(aPosition);
        if (rs.wasNull()) {
            return null;
        }

        var constants = returnedClass().getEnumConstants();
        if (dbValue >= 0 && dbValue < constants.length) {
            return constants[dbValue];
        }

        throw new IllegalStateException(
                "Unknown " + returnedClass().getSimpleName() + " value [" + dbValue + "]");
    }

    @Override
    public void nullSafeSet(PreparedStatement st, T value, int index,
            SharedSessionContractImplementor session)
        throws HibernateException, SQLException
    {
        if (value == null) {
            st.setNull(index, Types.INTEGER);
        }
        else {
            var dbValue = ArrayUtils.indexOf(returnedClass().getEnumConstants(), value);
            st.setInt(index, dbValue);
        }
    }

    @Override
    public T replace(T original, T target, Object owner) throws HibernateException
    {
        return original;
    }

    @Override
    public abstract Class<T> returnedClass();

    @Override
    public int getSqlType()
    {
        return Types.INTEGER;
    }
}
