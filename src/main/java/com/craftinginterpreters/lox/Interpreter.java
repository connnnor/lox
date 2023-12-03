package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object> {

    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /**
     * Convert object to String.
     *
     * Two edge cases are:
     * Converting null to "nil"
     * Stripping ".0" from string for integer-valued doubles
     * @param object object to stringify
     * @return object's string representation
     */
    private String stringify(Object object) {
        if (object == null) { return "nil"; }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }


    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            /* Comparison Operators */
            case GREATER:
                checkNumberOperand(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperand(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double)left <= (double)right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            /* Arithmetic Operators */
            // + is a little special since it is also used for concatenating strings
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");
            case MINUS:
                checkNumberOperand(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperand(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperand(expr.operator, left, right);
                return (double)left * (double)right;
        }
        // unreachable
        return null;
    }

    /**
     * Lox's notion of equality is basically the same as java's.
     * @param a object to compare
     * @param b object to compare
     * @return object's equality
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) { return true; }
        if (a == null) { return false; }

        return a.equals(b);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    /**
     * Evaluate literal.
     *
     * @param expr literal expression to evaluate.
     * @return literal's value
     */
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
        }
        // unreachable
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) { return; }
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperand(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double ) { return; }
        throw new RuntimeError(operator, "Operands must be a number");
    }

    /**
     * false and nil are false-y. everything else is truthy.
     *
     * @param object object to check
     * @return truthiness of object
     */
    private boolean isTruthy(Object object) {
        if (object == null) { return false; }
        if (object instanceof Boolean) {return (Boolean) object; }
        return true;
    }

    /** Helper method that sends expression back into visitor implementation. */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
}