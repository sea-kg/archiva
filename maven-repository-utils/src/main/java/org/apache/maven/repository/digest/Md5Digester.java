package org.apache.maven.repository.digest;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.security.NoSuchAlgorithmException;

/**
 * Digester that does MD5 Message Digesting Only.
 * 
 * @plexus.component role="org.apache.maven.repository.digest.Digester" role-hint="md5"
 */
public class Md5Digester
    extends AbstractDigester
{
    public Md5Digester()
        throws NoSuchAlgorithmException
    {
        super( new StreamingMd5Digester() );
    }
}
