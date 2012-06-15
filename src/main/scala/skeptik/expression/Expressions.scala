package skeptik.expression

import skeptik.judgment.Judgment
 
  
abstract class E extends Judgment {
  def t: T
    
  // ToDo: should think whether this is really needed.
  def copy: E
    
  //ToDo: should call canEqual
  //alphaEquals
  def =+=(e:E) = {
    def rec(e1:E,e2:E,map:Map[Var,Var]): Boolean = (e1,e2) match {
      case (v1:Var, v2:Var) => map.getOrElse(v1,v1)==v2
      case (Abs(v1@Var(_,t1),b1),Abs(v2@Var(_,t2),b2)) => {
        if (v1 == v2) rec(b1, b2, map)
        else if (t1 == t2) rec(b1, b2, map.updated(v1,v2))
        else false
      }
      case (App(f1,a1),App(f2,a2)) => rec(f1, f2, map) && rec(a1, a2, map)
      case _ => false
    }
    rec(this, e, Map())
  }
  def occursIn(e:E):Boolean = if (this == e) true else e match {
    case v: Var => false
    case App(f,a) => (this occursIn f) || (this occursIn a)
    case Abs(v,g) => (this occursIn v) || (this occursIn g)
  }
  // ToDo: maybe substitutions should be moved outside this class. 
  // They should be programmed as an internal DSL, similar to positions. 
  def /(subs: Map[Var,E]) =  {
    def rec(f:E,boundVars:Set[Var]):E = f match {
      case App(e1, e2) => App(rec(e1,boundVars),rec(e2,boundVars))
      case Abs(v,e) => Abs(v.copy, rec(e, boundVars + v))
      case v: Var if (boundVars contains v) => v.copy 
      case v: Var if (subs contains v) => subs(v).copy
      case v: Var => v.copy
    }
    rec(this, Set())
  }
}
case class Var(val name: String, override val t:T) extends E {
  override def copy = new Var(name,t)
  override def toString = name
}
case class Abs(val variable: Var, val body: E) extends E {
  def copy = new Abs(variable.copy,body.copy)
  override lazy val t = variable.t -> body.t
  override def toString = "@" + variable.name + ":" + variable.t + "." + body
}
case class App(val function: E, val argument: E) extends E {
  require(function.t.asInstanceOf[arrow].t1 == argument.t)
  def copy = new App(function.copy,argument.copy)
  override lazy val t = function.t.asInstanceOf[arrow].t2
  override def toString = "(" + function + " " + argument + ")"
}

