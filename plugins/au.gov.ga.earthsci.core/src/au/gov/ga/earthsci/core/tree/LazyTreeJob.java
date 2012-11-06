package au.gov.ga.earthsci.core.tree;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import au.gov.ga.earthsci.core.util.DelegatingProgressMonitor;

/**
 * An extension of the {@link Job} class that is used when expanding
 * {@link ILazyTreeNode}s.
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
public abstract class LazyTreeJob extends Job
{

	private DelegatingProgressMonitor monitor;
	
	final private Set<IProgressMonitor> additionalMonitors = new LinkedHashSet<IProgressMonitor>();
	final private ReadWriteLock monitorsLock = new ReentrantReadWriteLock();
	
	final private ILazyTreeNode<?> node;
	
	public LazyTreeJob(final ILazyTreeNode<?> node)
	{
		super(node.getName());
		this.node = node;
	}

	@Override
	protected final IStatus run(final IProgressMonitor monitor)
	{
		final DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
		delegatingMonitor.addMonitors(additionalMonitors);
		delegatingMonitor.addMonitor(monitor);
		
		// Add a cleanup monitor
		delegatingMonitor.addMonitor(new NullProgressMonitor() {
			@Override
			public void done()
			{
				LazyTreeJob.this.monitor = null;
			}
		});
		
		this.monitor = delegatingMonitor;
		
		return doRun(delegatingMonitor);
	}
	
	protected abstract IStatus doRun(IProgressMonitor monitor);
	
	/**
	 * @return The node that spawned this job
	 */
	public ILazyTreeNode<?> getNode()
	{
		return node;
	}

	/**
	 * Add an additional monitor delegate
	 */
	public void addMonitor(final IProgressMonitor monitor)
	{
		monitorsLock.writeLock().lock();
		try
		{
			additionalMonitors.add(monitor);
		}
		finally
		{
			monitorsLock.writeLock().unlock();
		}
		
		if (this.monitor != null)
		{
			this.monitor.addMonitor(monitor);
		}
	}
	
	/**
	 * Remove an additional monitor delegate
	 */
	public void removeMonitor(final IProgressMonitor monitor)
	{
		monitorsLock.writeLock().lock();
		try
		{
			additionalMonitors.remove(monitor);
		}
		finally
		{
			monitorsLock.writeLock().unlock();
		}
		
		if (this.monitor != null)
		{
			this.monitor.removeMonitor(monitor);
		}
	}

}
