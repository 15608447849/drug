// **********************************************************************
//
// Copyright (c) 2003-2016 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************
//
// Ice version 3.6.3
//
// <auto-generated>
//
// Generated from file `iceInterfaces.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

(function(module, require, exports)
{
    var Ice = require("ice").Ice;
    var __M = Ice.__M;
    var Slice = Ice.Slice;

    var inf = __M.module("inf");
    Slice.defineSequence(inf, "stringArrayHelper", "Ice.StringHelper", false);
    Slice.defineSequence(inf, "byteArrayHelper", "Ice.ByteHelper", true);

    /**
     * 方法参数
     **/
    inf.IParam = Slice.defineStruct(
        function(json, arrays, bytes, pageIndex, pageNumber, extend, token)
        {
            this.json = json !== undefined ? json : "";
            this.arrays = arrays !== undefined ? arrays : null;
            this.bytes = bytes !== undefined ? bytes : null;
            this.pageIndex = pageIndex !== undefined ? pageIndex : 0;
            this.pageNumber = pageNumber !== undefined ? pageNumber : 0;
            this.extend = extend !== undefined ? extend : "";
            this.token = token !== undefined ? token : "";
        },
        true,
        function(__os)
        {
            __os.writeString(this.json);
            inf.stringArrayHelper.write(__os, this.arrays);
            inf.byteArrayHelper.write(__os, this.bytes);
            __os.writeInt(this.pageIndex);
            __os.writeInt(this.pageNumber);
            __os.writeString(this.extend);
            __os.writeString(this.token);
        },
        function(__is)
        {
            this.json = __is.readString();
            this.arrays = inf.stringArrayHelper.read(__is);
            this.bytes = inf.byteArrayHelper.read(__is);
            this.pageIndex = __is.readInt();
            this.pageNumber = __is.readInt();
            this.extend = __is.readString();
            this.token = __is.readString();
        },
        13, 
        false);

    /**
     * 接口调用结构体
     **/
    inf.IRequest = Slice.defineStruct(
        function(pkg, cls, method, param)
        {
            this.pkg = pkg !== undefined ? pkg : "";
            this.cls = cls !== undefined ? cls : "";
            this.method = method !== undefined ? method : "";
            this.param = param !== undefined ? param : new inf.IParam();
        },
        true,
        function(__os)
        {
            __os.writeString(this.pkg);
            __os.writeString(this.cls);
            __os.writeString(this.method);
            inf.IParam.write(__os, this.param);
        },
        function(__is)
        {
            this.pkg = __is.readString();
            this.cls = __is.readString();
            this.method = __is.readString();
            this.param = inf.IParam.read(__is, this.param);
        },
        16, 
        false);

    /**
     * 服务接口 interface
     **/
    inf.Interfaces = Slice.defineObject(
        undefined,
        Ice.Object, undefined, 1,
        [
            "::Ice::Object",
            "::inf::Interfaces"
        ],
        -1, undefined, undefined, false);

    inf.InterfacesPrx = Slice.defineProxy(Ice.ObjectPrx, inf.Interfaces.ice_staticId, undefined);

    Slice.defineOperations(inf.Interfaces, inf.InterfacesPrx,
    {
        "accessService": [, , , , , [7], [[inf.IRequest]], , , , ]
    });
    exports.inf = inf;
}
(typeof(global) !== "undefined" && typeof(global.process) !== "undefined" ? module : undefined,
 typeof(global) !== "undefined" && typeof(global.process) !== "undefined" ? require : this.Ice.__require,
 typeof(global) !== "undefined" && typeof(global.process) !== "undefined" ? exports : this));
