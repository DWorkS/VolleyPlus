/**
 * Copyright 2013 Ognyan Bankov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.volley.demo.util;

import com.google.gson.annotations.SerializedName;


public class MyClass {
    @SerializedName("object_or_array")
    public String mType;
    
    @SerializedName("empty")
    public boolean mIsEmpty;
    
    @SerializedName("parse_time_nanoseconds")
    public long mNanoseconds;
    
    @SerializedName("validate")
    public boolean mIsValid;
}
