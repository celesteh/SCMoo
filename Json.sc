MooCustomEncoder {

	*new {
		^super.newCopyArgs()
	}

	value {|item, converter|

		"running the encoder".debug(this);
		item.notNil.if({
			item.respondsTo(\toJSON).if({
				"toJson incoming".debug(this);
				^item.toJSON(converter)
			});
		});
		^nil
	}
}

JSONConverter : JSONlib {

	var queue, done, inTree;


	*new { |postWarnings = true, useEvent=true, customEncoder=nil, customDecoder=nil|
		^super.new.init(postWarnings, useEvent, customEncoder, customDecoder)
	}


	*convertToJSON {|object, customEncoder=nil, postWarnings=true|
		if((object.isKindOf(Dictionary) or: (object.isKindOf(SequenceableCollection))).not) {
			//Error("Can only convert a Dictonary/Event/Array to JSON but received %".format(object.class)).warn
			"Can only convert a Dictonary/Event/Array to JSON but received %".format(object.class).warn
		};
		customEncoder = customEncoder ? MooCustomEncoder();
		"customEncoder %".format(customEncoder).debug(this);
		^super.new.init(postWarnings, customEncoder: customEncoder).prConvertTree(object)
	}

	init {|postWarnings, useEvent, customEncoder, customDecoder|

		this.postWarnings = postWarnings;
		this.useEvent = useEvent;
		this.customEncoder = customEncoder;
		this.customDecoder;

		queue = [];
		done = [];
		inTree = false;

		"init".debug(this);
	}

	convertToJSON{|object|

		^this.prConvertToJson(object);
	}

	prConvertTree {|object|

		var str, obj, id;

		"prConvertTree".debug(this);

		(inTree.not && object.isKindOf(MooObject)).if({
			inTree = true;
			done = [object.id];
			str = "[" + object.toJSON(this) ;

			obj = queue.pop;
			{obj.notNil}.while({
				obj.isKindOf(MooObject).if({
					id = obj.id;
				}, {
					id = obj;
					obj = Moo.default.at(id);
				});

				(obj.notNil && done.includes(id)).not.if({
					str = str ++ "\n," + obj.toJSON(this);
				});

				obj = queue.pop;
			});

			str = str + "]";
			inTree = false;
		}, {

			str = this.prConvertToJson(object);
		});

		^str;
	}

	prConvertToJson{|object|

		var val, id;

		"prConvertToJson".debug(this);

		object.isKindOf(MooObject).if({
			id = object.id;
			done.includes(id).not.if({
				queue = queue ++ object;
			});

			^"{ \"id\": % }".format(super.prConvertToJson(id));
		} , {

			if(customEncoder.notNil) {
				"customEncoder is not nil".debug(this);
				val = customEncoder.value(object, this);
				"val is %".format(val).debug(this);

				if(val.notNil) {
					^val
				}
			};


			^super.prConvertToJson(object);
		});

	}



}


+ SharedResource {
	toJSON {|converter|
		"toJSON".debug(this);
		^converter.convertToJSON(this.value.value)
	}
}

+ SharedCV  {
	toJSON {|converter|
		"toJSON".debug(this);
		^converter.convertToJSON(this.value.value)
	}
}

