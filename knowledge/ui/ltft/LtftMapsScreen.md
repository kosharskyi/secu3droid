## Wire rows increase with load, and the highest row appears at the top

Each correction index is `loadIndex * 16 + rpmIndex`. Reverse the rows only for display. The domain returns fractional indices in wire order, so the marker inverts its vertical coordinate and interpolates between cell centers rather than snapping to a cell. The frozen axis labels share the data grid's corresponding scroll position.
