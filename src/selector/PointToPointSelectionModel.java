package selector;

import java.awt.Point;
import java.util.ListIterator;

/**
 * Models a selection tool that connects each added point with a straight line.
 */
public class PointToPointSelectionModel extends SelectionModel {

    public PointToPointSelectionModel(boolean notifyOnEdt) {
        super(notifyOnEdt);
    }

    public PointToPointSelectionModel(SelectionModel copy) {
        super(copy);
    }

    /**
     * Return a straight line segment from our last point to `p`.
     */
    @Override
    public PolyLine liveWire(Point p) {
      

        PolyLine result = new PolyLine(lastPoint(), p);
        return result;
    }

    /**
     * Append a straight line segment to the current selection path connecting its end with `p`.
     */
    @Override
    protected void appendToSelection(Point p) {
      

        PolyLine newLine = new PolyLine(lastPoint(), p);
        selection.add(newLine);
    }

    /**
     * Move the starting point of the segment of our selection with index `index` to `newPos`,
     * connecting to the end of that segment with a straight line and also connecting `newPos` to
     * the start of the previous segment (wrapping around) with a straight line (these straight
     * lines replace both previous segments).  Notify listeners that the "selection" property has
     * changed.
     */
    @Override
    public void movePoint(int index, Point newPos) {
        // Confirm that we have a closed selection and that `index` is valid
        if (state() != SelectionState.SELECTED) {
            throw new IllegalStateException("May not move point in state " + state());
        }
        if (index < 0 || index >= selection.size()) {
            throw new IllegalArgumentException("Invalid segment index " + index);
        }

     
        ListIterator<PolyLine> iterator = selection.listIterator(index);
        PolyLine beforeSegment;
        PolyLine afterSegment;

        afterSegment = new PolyLine(newPos, iterator.next().end());
        iterator.set(afterSegment);
        iterator.previous();

        if (index == 0) {
         
            while (iterator.hasNext()) { iterator.next(); }

            start = new Point (newPos);
        }

        beforeSegment = new PolyLine(iterator.previous().start(), newPos);
        iterator.set(beforeSegment);

        propSupport.firePropertyChange("selection", null, selection());
    }
}
