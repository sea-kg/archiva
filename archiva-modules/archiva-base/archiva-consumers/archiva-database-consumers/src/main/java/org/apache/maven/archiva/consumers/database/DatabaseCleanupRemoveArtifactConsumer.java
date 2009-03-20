package org.apache.maven.archiva.consumers.database;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByArtifactConstraint;
import org.apache.maven.archiva.database.updater.DatabaseCleanupConsumer;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.audit.AuditEvent;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.io.File;

/**
 * Consumer for cleaning up the database of artifacts that are no longer existing in the repository. 
 *
 *         <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.database.updater.DatabaseCleanupConsumer"
 *                   role-hint="not-present-remove-db-artifact"
 *                   instantiation-strategy="per-lookup"
 */
public class DatabaseCleanupRemoveArtifactConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseCleanupConsumer
{
    /**
     * @plexus.configuration default-value="not-present-remove-db-artifact"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Remove artifact from database if not present on filesystem."
     */
    private String description;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArtifactDAO artifactDAO;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private RepositoryProblemDAO repositoryProblemDAO;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;
    
    private Logger logger = LoggerFactory.getLogger( "org.apache.archiva.AuditLog" );
    
    private static final char DELIM = ' ';

    public void beginScan()
    {
        // TODO Auto-generated method stub

    }

    public void completeScan()
    {
        // TODO Auto-generated method stub
    }

    public List<String> getIncludedTypes()
    {   
        return null;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        try
        {
            ManagedRepositoryContent repositoryContent =
                repositoryFactory.getManagedRepositoryContent( artifact.getModel().getRepositoryId() );

            File file = new File( repositoryContent.getRepoRoot(), repositoryContent.toPath( artifact ) );

            if( !file.exists() )
            {                    
                artifactDAO.deleteArtifact( artifact );
                
                triggerAuditEvent( repositoryContent.getRepository().getId(), artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion(), AuditEvent.REMOVE_SCANNED );

                // Remove all repository problems related to this artifact
                Constraint artifactConstraint = new RepositoryProblemByArtifactConstraint( artifact );
                List<RepositoryProblem> repositoryProblems =
                    repositoryProblemDAO.queryRepositoryProblems( artifactConstraint );

                if ( repositoryProblems != null )
                {
                    for ( RepositoryProblem repositoryProblem : repositoryProblems )
                    {
                        repositoryProblemDAO.deleteRepositoryProblem( repositoryProblem );
                    }
                }
            }
        }
        catch ( RepositoryException re )
        {
            throw new ConsumerException( "Can't run database cleanup remove artifact consumer: " + 
                                         re.getMessage() );
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new ConsumerException( e.getMessage() );
        }
    }

    public String getDescription()
    {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void setArtifactDAO( ArtifactDAO artifactDAO)
    {
        this.artifactDAO = artifactDAO;
    }

    public void setRepositoryProblemDAO( RepositoryProblemDAO repositoryProblemDAO )
    {
        this.repositoryProblemDAO = repositoryProblemDAO;
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }
    
    private void triggerAuditEvent( String repoId, String resource, String action )
    {
        String msg = repoId + DELIM + "<db-scan>" + DELIM + "<system>" + DELIM + '\"' + resource + '\"' +
            DELIM + '\"' + action + '\"';
        
        logger.info( msg );
    }
}
