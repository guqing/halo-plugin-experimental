package run.halo.app.extensions.extpoint;

import java.util.Iterator;

/**
 * {@link Iterator} that adapts the values returned from another iterator.
 *
 * @author guqing
 * @since 2021-11-08
 */
public abstract class Iterators<T, U> implements Iterator<U> {

    private final Iterator<? extends T> core;

    protected Iterators(Iterator<? extends T> core) {
        this.core = core;
    }

    protected Iterators(Iterable<? extends T> core) {
        this(core.iterator());
    }

    @Override
    public boolean hasNext() {
        return core.hasNext();
    }

    @Override
    public U next() {
        return expand(core.next());
    }

    protected abstract U expand(T item);

    @Override
    public void remove() {
        core.remove();
    }
}
