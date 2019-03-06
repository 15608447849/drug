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

package com.onek.server.inf;

/**
 * 服务接口 interface
 **/
public interface InterfacesPrx extends Ice.ObjectPrx
{
    public String accessService(IRequest request);

    public String accessService(IRequest request, java.util.Map<String, String> __ctx);

    public Ice.AsyncResult begin_accessService(IRequest request);

    public Ice.AsyncResult begin_accessService(IRequest request, java.util.Map<String, String> __ctx);

    public Ice.AsyncResult begin_accessService(IRequest request, Ice.Callback __cb);

    public Ice.AsyncResult begin_accessService(IRequest request, java.util.Map<String, String> __ctx, Ice.Callback __cb);

    public Ice.AsyncResult begin_accessService(IRequest request, Callback_Interfaces_accessService __cb);

    public Ice.AsyncResult begin_accessService(IRequest request, java.util.Map<String, String> __ctx, Callback_Interfaces_accessService __cb);

    public Ice.AsyncResult begin_accessService(IRequest request, 
                                               IceInternal.Functional_GenericCallback1<String> __responseCb, 
                                               IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    public Ice.AsyncResult begin_accessService(IRequest request, 
                                               IceInternal.Functional_GenericCallback1<String> __responseCb, 
                                               IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                               IceInternal.Functional_BoolCallback __sentCb);

    public Ice.AsyncResult begin_accessService(IRequest request, 
                                               java.util.Map<String, String> __ctx, 
                                               IceInternal.Functional_GenericCallback1<String> __responseCb, 
                                               IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb);

    public Ice.AsyncResult begin_accessService(IRequest request, 
                                               java.util.Map<String, String> __ctx, 
                                               IceInternal.Functional_GenericCallback1<String> __responseCb, 
                                               IceInternal.Functional_GenericCallback1<Ice.Exception> __exceptionCb, 
                                               IceInternal.Functional_BoolCallback __sentCb);

    public String end_accessService(Ice.AsyncResult __result);
}
