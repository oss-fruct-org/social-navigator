package org.fruct.oss.socialnavigator.test;

import android.test.AndroidTestCase;

import org.fruct.oss.socialnavigator.routing.ObstaclesIndex;
import org.fruct.oss.socialnavigator.utils.quadtree.Func;
import org.fruct.oss.socialnavigator.utils.quadtree.Node;
import org.fruct.oss.socialnavigator.utils.quadtree.QuadTree;

public class ObstaclesIndexTest extends AndroidTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testOnePoint() {
		QuadTree quadTree = new QuadTree(-90, -180, 90, 180);

		quadTree.set(61.78623, 34.356029, null);

		quadTree.navigate(quadTree.getRootNode(), new Func() {
			@Override
			public void call(QuadTree quadTree, Node node) {
				node.toString();
			}
		}, 61.784954, 34.354602, 61.787267, 34.357756);
	}
}
