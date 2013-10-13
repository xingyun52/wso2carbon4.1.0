grammar SiddhiQLGrammar;

options {
  language = Java;
  backtrack=true;
  output    = AST;
  ASTLabelType=CommonTree;
}

tokens {
  COLLECT;
  REGEX;
  HANDLERS;
  CONDITION;
  CONDITION_FUNCTION;
  EXTENSION_FUNCTION;
  OUT_STREAM;
  OUT_ATTRIBUTES;
  OUT_ATTRIBUTE;
  OUT_STREAM;
  SEQUENCE;
  PATTERN;
  JOIN; 
  STREAM;
  STREAM_DEFINITION;
  TABLE_DEFINITION;
  TABLE;
  QUERY;
  FUNCTION;
  PARAMETERS;
  ATTRIBUTE;
  IN_ATTRIBUTE;
  CONSTANT;
  ANONYMOUS;
  RETURN_QUERY;
  PATTERN_FULL;
  SEQUENCE_FULL;
  WINDOW;
  SIGNED_VAL;
  TIME_EXP;
  TRANSFORM;
  EXTENSION;
  FILTER;
  EXTENSION_FUNCTION;
}

@header {
	package org.wso2.siddhi.query.compiler;
	import java.util.LinkedList;
	import org.wso2.siddhi.query.compiler.exception.SiddhiPraserException;

}

@lexer::header {
	package org.wso2.siddhi.query.compiler;
	import org.wso2.siddhi.query.compiler.exception.SiddhiPraserException;

}


@parser::members {
  @Override
  public void emitErrorMessage(String errorMessage) {
    throw new SiddhiPraserException(errorMessage);
  }
}

@lexer::members {
  @Override
   public void emitErrorMessage(String errorMessage) {
     throw new SiddhiPraserException(errorMessage);
   }
}


executionPlan
	:(definitionStream|definitionTable|query) (';' (definitionStream|definitionTable|query))* ';'?  ->  (^(STREAM_DEFINITION definitionStream))*  (^(TABLE_DEFINITION definitionTable))*  ( query)*
	; 
   
definitionStream 
	:'define' 'stream' streamId '(' attributeName type (',' attributeName type )* ')'  ->  ^(streamId (^(IN_ATTRIBUTE attributeName type))+)
	;

definitionTable 
	:'define' 'table' id '(' attributeName type (',' attributeName type )* ')' ('from' tableType ':' databaseName '.' tableName )?  ->  ^(id (^(IN_ATTRIBUTE attributeName type))+ ^(TABLE  tableType databaseName tableName)? )
	;

query
	:inputStream outputStream outputProjection  ->  ^(QUERY outputStream inputStream outputProjection )
	|inputStream 'return' outputProjection  ->  ^(QUERY inputStream outputProjection )
	;

outputStream
	:'insert' outputType? 'into' streamId    ->   ^(OUT_STREAM streamId outputType?)
	;

outputType
	: 'expired-events' | 'current-events' | 'all-events'
	;

inputStream
	:'from' ( sequenceFullStream -> ^(SEQUENCE_FULL sequenceFullStream)
		| patternFullStream ->  ^(PATTERN_FULL  patternFullStream )
		| joinStream -> ^(JOIN joinStream) 
		| windowStream -> windowStream
		| basicStream  -> basicStream
		)
	;  
	
	 
patternFullStream
	:'(' patternStream ')' ('within' time)? ->  ^(PATTERN  patternStream  time? ) 
	|patternStream  ('within' time)?  ->  ^(PATTERN  patternStream  time? ) 
	;
 
basicStream
	: rawStream transformHandler? ('as' id)? -> ^(STREAM rawStream transformHandler?  id?)
	;

windowStream
	: streamId  filterHandler? transformHandler? windowHandler ('as' id)?-> ^(STREAM ^(streamId  filterHandler? transformHandler?  windowHandler) id?)
	| '(' returnQuery ')'  filterHandler? transformHandler? windowHandler ('as' id)?  ->  ^(STREAM ^( ANONYMOUS returnQuery   filterHandler? transformHandler?  windowHandler) id?)
	;

rawStream
	: streamId  filterHandler?   -> ^(streamId   filterHandler?  )
	| '(' returnQuery ')'  filterHandler?  ->  ^( ANONYMOUS returnQuery filterHandler? )
	;

joinStream
	:leftStream join rightStream 'unidirectional' ('on' condition)? ('within' time)? -> leftStream  join rightStream 'unidirectional' condition? time?
	|leftStream join rightStream ('on' condition)? ('within' time)? ->  leftStream  join rightStream condition? time?
	|leftStream 'unidirectional' join rightStream ('on' condition)? ('within' time)? -> leftStream 'unidirectional'  join  rightStream condition? time?
	;

leftStream
    :  windowStream
    | basicStream
    ;

rightStream
    :  windowStream
    |  basicStream
    ;

returnQuery
	: inputStream 'return' outputProjection	-> ^(RETURN_QUERY  inputStream outputProjection)
	;

patternStream
	: patternItem ( FOLLOWED_BY patternStream )?  ->   patternItem patternStream?
	| 'every' patternItem ( FOLLOWED_BY patternStream )?  ->  ^( 'every'  patternItem ) patternStream?
	| 'every' '('nonEveryPatternStream')' ( FOLLOWED_BY patternStream )? -> ^( 'every' nonEveryPatternStream )   patternStream?
	;

nonEveryPatternStream
	: patternItem  ( FOLLOWED_BY nonEveryPatternStream )?  ->  patternItem nonEveryPatternStream?
	;

sequenceFullStream
	:sequenceStream ('within' time)? ->  ^(SEQUENCE  sequenceStream time? )
	;

sequenceStream
	: sequenceItem ',' sequenceItem  (',' sequenceItem )*   ->  sequenceItem+
	;

FOLLOWED_BY
	: '->'/*|'-['countEnd']>'*/
	;

patternItem
	: itemStream 'and'^ itemStream
	| itemStream 'or'^ itemStream
	| itemStream '<'collect '>' -> ^(COLLECT itemStream collect)
	| itemStream
	;

sequenceItem
	: itemStream 'or'^ itemStream
	| itemStream regex -> ^(REGEX itemStream regex)
	| itemStream
	;

itemStream
	: attributeName'='rawStream  ->   ^(STREAM rawStream attributeName?)
	;

regex
	: ('*'|'+'|'?') '?'?
	;

outputProjection
	: externalCall? outputAttributeList groupBy? having? ->  externalCall? ^(OUT_ATTRIBUTES outputAttributeList ) groupBy? having?
	;

outputAttributeList
	:'*'
	| outputItem (',' outputItem)* ->( ^(OUT_ATTRIBUTE outputItem))+
	|-> '*'
	;

outputItem
	: extensionOutFunction 'as' id ->   id extensionOutFunction
	| outFunction 'as' id ->  outFunction id
	| expression  'as' id  ->   expression id
	| attributeVariable
	;

extensionOutFunction
	: extensionId ':' functionId  '(' parameters? ')' -> ^( EXTENSION_FUNCTION extensionId functionId parameters?)
	;

outFunction
	: ID '(' parameters? ')' -> ^( FUNCTION ID parameters?)
	;

groupBy
	: 'group' 'by' attributeVariable (',' attributeVariable)*  ->   ^('group' attributeVariable+)
	;

having
	: 'having' condition  -> ^('having' condition)
	;

externalCall
	: 'call' ID '(' parameters? ')'  ->  ^( 'call' ^(ID parameters?))
	;

filterHandler
	: '[' condition ']'  ->    ^( FILTER condition)
	;

transformHandler
	: '#' transform '.' extensibleId  ('(' parameters? ')')?  ->   ^( TRANSFORM extensibleId parameters?)
	;

windowHandler
	: '#' window  '.' extensibleId  ('(' parameters? ')')?  ->   ^( WINDOW extensibleId parameters?)
	;

extensibleId
	: extensionId ':' functionId ->  ^( EXTENSION  extensionId functionId)
	| id
	;

parameters
	: parameter (',' parameter)*  ->  ^(PARAMETERS parameter+)
	;

time
	: constant
	;
	
parameter
	: expression
	;

collect
	: countStart ':' countEnd
	| countStart ':'
	| ':' countEnd
	| countStartAndEnd
	;

countStart :POSITIVE_INT_VAL;

countEnd :POSITIVE_INT_VAL;

countStartAndEnd :POSITIVE_INT_VAL;

//conditions start

condition
	:conditionExpression  -> ^(CONDITION conditionExpression)
	;

conditionExpression
   	: andCondition ('or'^ conditionExpression )?
	;

andCondition
	: compareCondition ('and'^ conditionExpression)?
	;

compareCondition
	:expression compareOperation^ expression
	|boolVal
    |'('conditionExpression ')' -> conditionExpression
    |notCondition
    |extensionCondition
	;

expression
   	:minusExpression ('+'^ expression)?
   	;

minusExpression
   	:multiplyExpression ('-'^ minusExpression)?
   	;

multiplyExpression
   	:divisionExpression ('*'^ multiplyExpression)?
   	;

divisionExpression
   	:modExpression ('/'^ divisionExpression)?
   	;

modExpression
    :valueExpression ('%'^ modExpression)?
    ;

valueExpression
    : constant
    | attributeVariable
    | type
    | '('expression ')' -> expression
    | extensionExpression
    ;

notCondition
	:'not' '('conditionExpression')' ->  ^('not' conditionExpression)
	;

extensionCondition
    :   extensionId ':' functionId  ('(' parameters? ')')?  ->   ^( CONDITION_FUNCTION extensionId functionId parameters?)
    ;

extensionExpression
    :   extensionId ':' functionId  ('(' parameters? ')')?  ->   ^( EXTENSION_FUNCTION extensionId functionId parameters?)
    ;

//conditions end

constant
	:intVal -> ^( CONSTANT intVal)
	|longVal -> ^( CONSTANT longVal)
	|floatVal  -> ^( CONSTANT floatVal)
	|doubleVal -> ^( CONSTANT doubleVal)
	|boolVal -> ^( CONSTANT boolVal)
	|stringVal -> ^( CONSTANT stringVal)
	|timeExpr   -> ^( CONSTANT timeExpr)
	;

streamId: id;

attributeVariable
	:streamPositionAttributeName|streamAttributeName|attributeName;

streamPositionAttributeName
	:streamId '['POSITIVE_INT_VAL']''.' id ->  ^( ATTRIBUTE ^(streamId POSITIVE_INT_VAL) id)
	;

streamAttributeName
	: streamId '.' id  ->  ^( ATTRIBUTE streamId id)
	;

attributeName
	: id  ->  ^( ATTRIBUTE id)
	;

join
	: 'left''outer' 'join' ->  ^('join' ^('outer' 'left'))
	| 'right' 'outer' 'join' -> ^('join' ^('outer' 'right'))
	| 'full''outer' 'join' -> ^('join' ^('outer' 'full'))
	| 'outer' 'join'  -> ^('join' ^('outer' 'full'))
	| 'inner' 'join'  -> ^('join' 'inner')
	|  'join' -> ^('join' 'inner')
	;

window
    : 'window'
    ;

transform
    : 'transform'
    ;

compareOperation
	:'==' |'!=' |'<='|'>=' |'<' |'>'  |'contains' | 'instanceof'
	;

id: ID|ID_QUOTES ;

timeExpr
    : (yearValue)? (monthValue)? (weekValue)? (dayValue)? (hourValue)? (minuteValue)? (secondValue)?  (milliSecondValue)?
	-> ^(TIME_EXP yearValue? monthValue? weekValue? dayValue? hourValue? minuteValue? secondValue? milliSecondValue?  )
	;

yearValue
	: POSITIVE_INT_VAL ( 'years' | 'year')
	;

monthValue
	: POSITIVE_INT_VAL ( 'months' | 'month')
	;

weekValue
	: POSITIVE_INT_VAL ( 'weeks' | 'week')
	;

dayValue
	: POSITIVE_INT_VAL ( 'days' | 'day')
	;

hourValue
	: POSITIVE_INT_VAL ( 'hours' |   'hour' )
	;

minuteValue
	: POSITIVE_INT_VAL ( 'minutes' |  'min'  | 'minute'  )
	;

secondValue
	: POSITIVE_INT_VAL ('seconds' | 'second' | 'sec'  )
	;

milliSecondValue  returns [long value]
	: POSITIVE_INT_VAL ( 'milliseconds' |  'millisecond'  )
	;

intVal: '-'? POSITIVE_INT_VAL -> ^(SIGNED_VAL  POSITIVE_INT_VAL '-'?);

longVal: '-'? POSITIVE_LONG_VAL -> ^(SIGNED_VAL  POSITIVE_LONG_VAL '-'?);

floatVal: '-'? POSITIVE_FLOAT_VAL -> ^(SIGNED_VAL POSITIVE_FLOAT_VAL  '-'?);

doubleVal: '-'? POSITIVE_DOUBLE_VAL -> ^(SIGNED_VAL  POSITIVE_DOUBLE_VAL '-'?);

boolVal: BOOL_VAL;

extensionId: id;

functionId: id;

tableType: id;

databaseName: id;

tableName: id;

stringVal: STRING_VAL;

type: 'string' |'int' |'long' |'float' |'double' |'bool';

POSITIVE_INT_VAL:  NUM('I'|'i')?;

POSITIVE_LONG_VAL:  NUM ('L'|'l');

POSITIVE_FLOAT_VAL:  NUM ('.' NUM)? NUM_SCI? ('F'|'f');

POSITIVE_DOUBLE_VAL : NUM ('.' NUM NUM_SCI? ('D'|'d')?| NUM_SCI? ('D'|'d'));

BOOL_VAL: ('true'|'false');

//Need to be in the top to get high priority
ID_QUOTES : '`'('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*'`' {setText(getText().substring(1, getText().length()-1));};

ID : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')* ;

//('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'-'|','|' '|'\t')*

STRING_VAL
	:'\'' ( ~('\u0000'..'\u001f' | '\\' | '\''| '\"' ) )* '\'' {setText(getText().substring(1, getText().length()-1));}
	|'"' ( ~('\u0000'..'\u001f' | '\\'  |'\"') )* '"'          {setText(getText().substring(1, getText().length()-1));}
	;	

fragment NUM: '0'..'9'+;

fragment NUM_SCI: ('e'|'E') '-'? NUM;

//Hidden channels 

WS  : (' '|'\r'|'\t'|'\u000C'|'\n') {$channel=HIDDEN;}
    ;
COMMENT
    : '/*' .* '*/' {$channel=HIDDEN;}
    ;
LINE_COMMENT
    : '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    ;
