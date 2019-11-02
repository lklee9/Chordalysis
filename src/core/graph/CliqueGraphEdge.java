/*******************************************************************************
 * Copyright (C) 2015 Francois Petitjean
 * 
 * This file is part of Chordalysis.
 * 
 * Chordalysis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * Chordalysis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Chordalysis.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package core.graph;

import org.jgrapht.graph.DefaultEdge;

import java.util.BitSet;

public class CliqueGraphEdge extends DefaultEdge {
  private BitSet c1,c2,separator;
  public CliqueGraphEdge(){
  }

  public CliqueGraphEdge(BitSet c1, BitSet c2){
    this.c1 = c1;
    this.c2 = c2;
    this.store();
  }

  private void store() {
    if (this.c1 == null) this.c1 = (BitSet) this.getSource();
    if (this.c2 == null) this.c2 = (BitSet) this.getTarget();
    if (this.separator == null) {
      this.separator = (BitSet) c1.clone();
      this.separator.and(c2);
    }
  }

  @Override
  public boolean equals(Object o) {
  	this.store();
    if (o == this)
      return true;
    if(o instanceof CliqueGraphEdge){
      CliqueGraphEdge e = (CliqueGraphEdge) o;

      return (c1.size()==e.c1.size() && c2.size()==e.c2.size() && e.c1.equals(c1) && e.c2.equals(c2))||
        (c1.size()==e.c2.size() && c2.size()==e.c1.size() && e.c1.equals(c2) && e.c2.equals(c1));
    }else{
      return false;
    }
  }

  public String toString(){
    this.store();
    return c1.toString()+" inter "+c2.toString()+" = "+separator.toString();
  }

  public BitSet getClique1(){
    this.store();
    return (BitSet) c1.clone();
  }

  public BitSet getClique2(){
    this.store();
    return (BitSet) c2.clone();
  }

  public BitSet getSeparator(){
    this.store();
    return (BitSet) separator.clone();
  }

}
