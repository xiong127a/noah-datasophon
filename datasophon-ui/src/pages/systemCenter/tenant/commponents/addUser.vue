<template>
  <div style="padding-top: 10px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="p0-32-10-32 form-content"
      label-width="120px">
      <a-form-item label="租户名称">
        <a-input v-decorator="[
          'tenantName',
          { rules: [{ required: true, message: '租户名称不能为空!' }, { validator: checkName }] },
        ]" placeholder="请输入租户名称" />
      </a-form-item>
      <a-form-item label="描述">
        <a-input placeholder="请输入描述，500个字符以内" type="textarea" />
      </a-form-item>
      <a-form-item label="选择组件">
        <a-tabs type="card" v-model="tabType" @change="changeTab">
          <a-tab-pane key="hdfsResourceList" tab="HDFS"></a-tab-pane>
          <a-tab-pane key="yarnResourceList" tab="YARN"></a-tab-pane>
          <a-tab-pane key="hbaseResourceList" tab="HBASE"></a-tab-pane>
          <a-tab-pane key="hiveResourceList" tab="HIVE"></a-tab-pane>
          <a-tab-pane key="kafkaResourceList" tab="KAFKA"></a-tab-pane>
        </a-tabs>
        <div class="module">
          <div class="titleTop">
            <span>组件名</span>
            <span>申请项</span>
          </div>
          <div class="moduleItems" v-show="resourceList.hdfsResourceList.length > 0">
            <div class="name">HDFS </div>
            <div class="right">
              <div class="title">
                <span>路径</span>
                <span>存储空间配额(GB)</span>
              </div>
              <div class="content" v-for="(item, index) in resourceList.hdfsResourceList" :key="index">
                <a-input style="width: 40%;margin:5px" v-model="item.hdfsPath"
                  :readOnly="editFlag && item.type == undefined"></a-input>
                <a-input style="width: 40%;margin:5px" v-model="item.hdfsSpaceQuota"></a-input>
                <a-icon type="minus-square" style="color: #db0315;" @click="toDelete('hdfsResourceList', index)"></a-icon>
              </div>
              <a-icon type="plus-square" class="plusIcon" @click="toAdd('hdfsResourceList')" />
            </div>
            <div class="clearDom"> <a-icon type="delete" class="clearIcon" @click="toClear('hdfsResourceList')"></a-icon>
            </div>
          </div>
          <div class="moduleItems" v-show="resourceList.yarnResourceList.length > 0">
            <div class="name">YARN </div>
            <div class="right">
              <div class="title">
                <span>父队列名称</span>
                <span>队列名称</span>
                <span>占比</span>
                <span>标签</span>
              </div>
              <div class="content" v-for="(item, index) in resourceList.yarnResourceList" :key="index">
                <a-input style="width: 24%;margin:5px" v-model="item.parentQueueName"
                  :readOnly="editFlag && item.type == undefined"></a-input>
                <a-input style="width: 24%;margin:5px" v-model="item.queueName"
                  :readOnly="editFlag && item.type == undefined"></a-input>
                <a-input style="width: 24%;margin:5px" v-model="item.capacityPercent"></a-input>
                <a-input style="width: 24%;margin:5px" v-model="item.nodeLabel"></a-input>
                <a-icon type="minus-square" style="color: #db0315;" @click="toDelete('yarnResourceList', index)"></a-icon>
              </div>
              <a-icon type="plus-square" class="plusIcon" @click="toAdd('yarnResourceList')" />
            </div>
            <div class="clearDom"> <a-icon type="delete" class="clearIcon" @click="toClear('yarnResourceList')"></a-icon>
            </div>
          </div>
          <div class="moduleItems" v-show="resourceList.hbaseResourceList.length > 0">
            <div class="name">HBASE </div>
            <div class="right">
              <div class="title">
                <span>命名空间</span>
                <span>容量(GB)</span>
                <span>RegionServer数量(个)</span>
              </div>
              <div class="content" v-for="(item, index) in resourceList.hbaseResourceList" :key="index">
                <a-input style="width: 30%;margin:5px" v-model="item.hbaseNamespace"
                  :readOnly="editFlag && item.type == undefined"></a-input>
                <a-input style="width: 30%;margin:5px" v-model="item.hbaseCapacity"></a-input>
                <a-input style="width: 30%;margin:5px" v-model="item.hbaseRegionServerNum"></a-input>
                <a-icon type="minus-square" style="color: #db0315;"
                  @click="toDelete('hbaseResourceList', index)"></a-icon>
              </div>
              <a-icon type="plus-square" class="plusIcon" @click="toAdd('hbaseResourceList')" />
            </div>
            <div class="clearDom"> <a-icon type="delete" class="clearIcon" @click="toClear('hbaseResourceList')"></a-icon>
            </div>
          </div>
          <div class="moduleItems" v-show="resourceList.hiveResourceList.length > 0">
            <div class="name">HIVE </div>
            <div class="right">
              <div class="title">
                <span>数据库名</span>
                <span>数据库容量</span>
              </div>
              <div class="content" v-for="(item, index) in resourceList.hiveResourceList" :key="index">
                <a-input style="width: 40%;margin:5px" v-model="item.hiveDatabase"
                  :readOnly="editFlag && item.type == undefined"></a-input>
                <a-input style="width: 40%;margin:5px" v-model="item.hiveDatabaseCapacity"></a-input>
                <a-icon type="minus-square" style="color: #db0315;" @click="toDelete('hiveResourceList', index)"></a-icon>
              </div>
              <a-icon type="plus-square" class="plusIcon" @click="toAdd('hiveResourceList')" />
            </div>
            <div class="clearDom"> <a-icon type="delete" class="clearIcon" @click="toClear('hiveResourceList')"></a-icon>
            </div>
          </div>
          <div class="moduleItems" v-show="resourceList.kafkaResourceList.length > 0">
            <div class="name">KAFKA </div>
            <div class="right">
              <div class="title">
                <span>topic名称</span>
                <span>存储容量(GB)</span>
                <span>副本数(个)</span>
              </div>
              <div class="content" v-for="(item, index) in resourceList.kafkaResourceList" :key="index">
                <a-input style="width: 30%;margin:5px" v-model="item.kafkaTopicName"
                  :readOnly="editFlag && item.type == undefined"></a-input>
                <a-input style="width: 30%;margin:5px" v-model="item.kafkaTopicCapacity"></a-input>
                <a-input style="width: 30%;margin:5px" v-model="item.kafkaReplicas"></a-input>
                <a-icon type="minus-square" style="color: #db0315;"
                  @click="toDelete('kafkaResourceList', index)"></a-icon>
              </div>
              <a-icon type="plus-square" class="plusIcon" @click="toAdd('kafkaResourceList')" />
            </div>
            <div class="clearDom"> <a-icon type="delete" class="clearIcon" @click="toClear('kafkaResourceList')"></a-icon>
            </div>
          </div>
        </div>
      </a-form-item>
    </a-form>
    <div class="ant-modal-confirm-btns-new">
      <a-button style="margin-right: 10px" type="primary" @click.stop="handleSubmit" :loading="loading">确认</a-button>
      <a-button @click.stop="formCancel">取消</a-button>
    </div>
  </div>
</template> 
<script>
export default {
  props: {
    detail: {
      type: Object,
      default: function () {
        return {};
      },
    },
    callBack: Function,
  },
  data () {
    return {
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      editFlag: false,
      tabType: 'hdfsResourceList',
      labelCol: {
        xs: { span: 6 },
        sm: { span: 2 },
      },
      wrapperCol: {
        xs: { span: 18 },
        sm: { span: 18 },
      },
      resourceListINIT: {
        "hdfsResourceList": [
          {
            "hdfsSpaceQuota": "21",
            "hdfsPath": "/tenant/test11",
            "serviceName": "HDFS",
            'name': 'aa',

          },
          {
            "hdfsPath": "/tenant/test2",
            "hdfsSpaceQuota": "2",

            "serviceName": "HDFS",

          }
        ],
        "yarnResourceList": [
          {

            "queueName": "tenantTest1",
            "capacityPercent": "1022",
            "nodeLabel": "default",
            "serviceName": "YARN",

            "parentQueueName": "root",
          }
        ],
        "hiveResourceList": [
          {

            "hiveDatabaseCapacity": "22",
            "hiveDatabase": "tenantTest1",
            "serviceName": "HIVE",

          }
        ],
        "hbaseResourceList": [
          {
            "hbaseNamespace": "tenantTest1",
            "hbaseCapacity": "2",
            "hbaseRegionServerNum": "3",
            "serviceName": "HBASE",

          }
        ],
        "kafkaResourceList": [
          {

            "kafkaTopicName": "tenantTest12",
            "kafkaTopicCapacity": "2",
            "kafkaReplicas": "32",
            "serviceName": "KAFKA",

          }
        ]
      },
      resourceList: {
        "hdfsResourceList": [{
          "hdfsPath": "",
          "hdfsSpaceQuota": "",
          "serviceName": "HDFS",
        }],
        "yarnResourceList": [],
        "hiveResourceList": [],
        "hbaseResourceList": [],
        "kafkaResourceList": []
      },
      resourceList1: {
        "hdfsResourceList": [
          {
            "serviceName": "HDFS",
            "type": "ADD",
            "hdfsPath": "/tenant/test1",
            "hdfsSpaceQuota": "2"
          },
          {
            "serviceName": "HDFS",
            "type": "ADD",
            "hdfsPath": "/tenant/test2",
            "hdfsSpaceQuota": "2"
          }
        ],
        "yarnResourceList": [
          {
            "serviceName": "YARN",
            "type": "ADD",
            "parentQueueName": "root",
            "queueName": "tenantTest1",
            "capacityPercent": "10",
            "nodeLabel": "default"
          }
        ],
        "hiveResourceList": [
          {
            "serviceName": "HIVE",
            "type": "ADD",
            "hiveDatabase": "tenantTest1",
            "hiveDatabaseCapacity": "2"
          }
        ],
        "hbaseResourceList": [
          {
            "serviceName": "HBASE",
            "type": "ADD",
            "hbaseNamespace": "tenantTest1",
            "hbaseCapacity": "2",
            "hbaseRegionServerNum": "3"
          }
        ],
        "kafkaResourceList": [
          {
            "serviceName": "KAFKA",
            "type": "ADD",
            "kafkaTopicName": "tenantTest1",
            "kafkaTopicCapacity": "2",
            "kafkaReplicas": "3"
          }
        ]
      },
      form: this.$form.createForm(this),
      loading: false,
    };
  },
  watch: {},
  methods: {
    changeTab (type) {
      if (this.resourceList[type].length == 0)
        switch (type) {
          case 'hdfsResourceList': this.resourceList[type] = [{
            "hdfsPath": "",
            "hdfsSpaceQuota": "",
            "serviceName": "HDFS",
            "type": 'ADD'
          }]; break
          case 'yarnResourceList': this.resourceList[type] = [{
            "parentQueueName": "",
            "queueName": "",
            "capacityPercent": "",
            "nodeLabel": "",
            "serviceName": "YARN",
            "type": 'ADD'
          }]; break
          case 'hiveResourceList': this.resourceList[type] = [{
            "hiveDatabase": "",
            "hiveDatabaseCapacity": "",
            "serviceName": "HIVE",
            "type": 'ADD'
          }]; break
          case 'hbaseResourceList': this.resourceList[type] = [{
            "hbaseNamespace": "",
            "hbaseCapacity": "",
            "hbaseRegionServerNum": "",
            "serviceName": "HBASE",
            "type": 'ADD'
          }]; break
          case 'kafkaResourceList': this.resourceList[type] = [{
            "kafkaTopicName": "",
            "kafkaTopicCapacity": "",
            "kafkaReplicas": "",
            "serviceName": "KAFKA",
            "type": 'ADD'
          }]; break
        }
    },
    toAdd (type) {
      switch (type) {
        case 'hdfsResourceList': this.resourceList[type].push({
          "hdfsPath": "",
          "hdfsSpaceQuota": "",
          "serviceName": "HDFS",
          "type": 'ADD'
        },); break
        case 'yarnResourceList': this.resourceList[type].push({
          "parentQueueName": "",
          "queueName": "",
          "capacityPercent": "",
          "nodeLabel": "",
          "serviceName": "YARN",
          "type": 'ADD'
        },); break
        case 'hiveResourceList': this.resourceList[type].push({
          "hiveDatabase": "",
          "hiveDatabaseCapacity": "",
          "serviceName": "HIVE", "type": 'ADD'
        },); break
        case 'hbaseResourceList': this.resourceList[type].push({
          "hbaseNamespace": "",
          "hbaseCapacity": "",
          "hbaseRegionServerNum": "",
          "serviceName": "HBASE", 
          "type": 'ADD'
        },); break
        case 'kafkaResourceList': this.resourceList[type].push({
          "kafkaTopicName": "",
          "kafkaTopicCapacity": "",
          "kafkaReplicas": "",
          "serviceName": "KAFKA", 
          "type": 'ADD'
        },); break
      }
    },
    toDelete (type, index) {
      if (this.resourceList[type].length == 1) {
        this.resourceList[type] = []
      } else {
        this.resourceList[type].splice(index, 1);
      }
    },
    toClear (type) {
      this.resourceList[type] = []
    },
    checkName (rule, value, callback) {
      var reg = /[\u4E00-\u9FA5]|[\uFE30-\uFFA0]/g;
      if (reg.test(value)) {
        // callback(new Error("名称中不能包含中文"));
      }
      if (/\s/g.test(value)) {
        callback(new Error("名称中不能包含空格"));
      }
      callback();
    },
    formCancel () {
      this.$destroyAll();
    },
    // 递归比较两个对象的所有属性是否相等
    objectsAreEqual (obj1, obj2, indexArr, resourceName) {
      // 获取两个对象的所有键  
      const keys1 = Object.keys(obj1);
      //第一个键值相等 其他不等 则是update
      if ((obj1[keys1[0]] == obj2[keys1[0]])) {
        // 遍历所有键并比较它们的值
        for (const key of keys1) {
          const val1 = obj1[key];
          const val2 = obj2[key];
          if (val1 !== val2) {
            this.resourceList[resourceName][indexArr].type = 'UPDATE'
          }
        }
        return true;
      }
    },
    // 检查两个数组是否包含相同的对象（属性相等）
    arraysHaveSameObjects (arr1, arr2, resourceName) {
      for (let i = 0; i < arr1.length; i++) {
        const obj1 = arr1[i];
        const obj2 = arr2.find((obj, indexArr) => this.objectsAreEqual(obj, obj1, indexArr, resourceName));
        // 如果在arr2中找不到与arr1当前对象相等的对象，返回false
        if (!obj2) {
          return false;
        }
      }

    },
    areObjectsEqual (obj1, obj2) {
      if (typeof obj1 !== typeof obj2) {
        return false;
      }
      // 如果不是对象，直接比较值  
      if (typeof obj1 !== 'object' || obj1 === null || obj2 === null) {
        return obj1 === obj2;
      }
      // 获取对象的键  
      const keys1 = Object.keys(obj1);
      const keys2 = Object.keys(obj2);
      if (keys1.length !== keys2.length) {
        return false;
      }
      // 比较每个键的值  
      for (let key of keys1) {
        if (!this.areObjectsEqual(obj1[key], obj2[key])) {
          return false;
        }
      }

      // 如果所有键的值都相等，则对象相等  
      return true;
    },
    compareArrays (array1, array2, resourceName) {
      // 判断arr1中的某项是否被删除
      let valueArr2 = new Set(array2.map(item => {
        return Object.values(item)[0]
      }));
      this.resourceListINIT[resourceName] = array1.map(item => {
        if (!valueArr2.has(Object.values(item)[0])) {
          return { ...item, type: 'DELETE' };
        }
        return item;
      });

      // 判断arr2是否有新增项
      let valueArr1 = new Set(array1.map(item => {
        return Object.values(item)[0]
      }));
      this.resourceList[resourceName] = array2.map(item => {
        if (!valueArr1.has(Object.values(item)[0])) {
          return { ...item, type: 'ADD' };
        }
        return item;
      });

      for (let i = 0; i < array1.length; i++) {
        array2.forEach((e, index) => {
          // 使用函数比较 obj1 和 obj2
          const areEqual = this.areObjectsEqual(array1[i], e);
          if (areEqual) {
            this.resourceList[resourceName][index].type = 'NONE'
            this.resourceListINIT[resourceName][i].type = 'NONE'
          }
        })
        this.arraysHaveSameObjects(array1, array2, resourceName);
      }
      return true;
    },
    handleSubmit (e) {
      const _this = this;
      e.preventDefault();
      this.form.validateFields((err, values) => {
        if (!err) {
          // 新增则每项添加 type:add
          if (_this.editFlag == false) {
            let resourceList = _this.resourceList
            for (const key in resourceList) {
              if (Array.isArray(resourceList[key])) {
                _this.resourceList[key] = resourceList[key].map(item => ({
                  ...item,
                  type: 'ADD'
                }));
              }
            }
          } else {
            // 定义要比较的资源列表名称
            const resourceNames = ["hdfsResourceList", "yarnResourceList", "hiveResourceList", "hbaseResourceList", "kafkaResourceList"];
            resourceNames.forEach(resourceName => {
              _this.compareArrays(_this.resourceListINIT[resourceName], _this.resourceList[resourceName], resourceName);
            });
            // 合并：arr1中被删除的合并到arr2
            resourceNames.forEach(resourceName => {
              this.resourceListINIT[resourceName].forEach(e => {
                if (e.type == 'DELETE') {
                  this.resourceList[resourceName].push(e)
                }
              })
            })

          };
          console.log('数据格式1', this.resourceListINIT);
          console.log('数据格式2', this.resourceList);
          const params = {
            clusterId: this.clusterId,
            id: _this.editFlag ? this.detail.id : '',
            tenantName: values.tenantName,
            ..._this.resourceList,
          };
          this.loading = true;
          this.$axiosJsonPost('/ddh/cluster/tenant/save', params)
            .then((res) => {
              this.loading = false;
              if (res.code === 200) {
                this.$message.success("保存成功", 2); 
                this.$destroyAll();
                _this.callBack();
              }
            })
            .catch((err) => { });
        }
      });
    },
    toFirst (obj, propertyName) {
      const firstProperty = obj[propertyName];
      const restProperties = Object.keys(obj).filter(key => key !== propertyName).reduce((acc, key) => {
        acc[key] = obj[key];
        return acc;
      }, {});
      return Object.assign({ [propertyName]: firstProperty }, { ...restProperties })
      // return {
      //   [propertyName]: firstProperty,
      //   ...restProperties,
      // };
    },
    keepOnlyDesiredProperties (array, desiredProperties) {
      return array.map(obj => {
        const newObj = {};
        desiredProperties.forEach(prop => {
          // obj 内是否有 prop属性
          if (Object.prototype.hasOwnProperty.call(obj, prop)) {
            newObj[prop] = obj[prop];
          }
        });
        return newObj;
      });
    },
    movePropertyToFirst (arr, propertyName, desiredProps, type, newName = propertyName) {
      // 示例用法  
      const objectsArray = [
        { a: 1, b: 2, c: 3 },
        { a: 4, b: 5, c: 6 },
        { a: 7, b: 8, c: 9 }
      ];

      // const desiredProps = ['a', 'b']; // 保留的属性列表  

      arr = this.keepOnlyDesiredProperties(arr, desiredProps);
      let newObjectsArray = this.keepOnlyDesiredProperties(arr, desiredProps);

      arr.forEach((obj, index) => {
        // 示例用法  
        const myObject = {
          b: 2,
          a: 1,
          c: 3
        };

        const newObject = this.toFirst(obj, propertyName);
        this.resourceListINIT[type][index] = newObject
      });
    },
    // 点击编辑
    echoUSer () {
      if (JSON.stringify(this.detail) !== "{}") {
        this.editFlag = true;
        this.form.setFieldsValue({ ...this.detail })
        this.resourceListINIT = { ...this.detail }
        const resourceNames = ["hdfsResourceList", "yarnResourceList", "hiveResourceList", "hbaseResourceList", "kafkaResourceList"];
        resourceNames.forEach(type => {
          switch (type) {
            case 'hdfsResourceList': this.movePropertyToFirst(this.resourceListINIT[type], 'hdfsPath', ['hdfsPath', 'hdfsSpaceQuota', 'serviceName',], type); break
            case 'yarnResourceList': this.movePropertyToFirst(this.resourceListINIT[type], 'parentQueueName', ['parentQueueName', 'queueName', 'capacityPercent', 'nodeLabel', 'serviceName',], type); break
            case 'hiveResourceList': this.movePropertyToFirst(this.resourceListINIT[type], 'hiveDatabase', ['hiveDatabase', 'hiveDatabaseCapacity', 'serviceName',], type); break
            case 'hbaseResourceList': this.movePropertyToFirst(this.resourceListINIT[type], 'hbaseNamespace', ['hbaseNamespace', 'hbaseCapacity', 'hbaseRegionServerNum', 'serviceName',], type); break
            case 'kafkaResourceList': this.movePropertyToFirst(this.resourceListINIT[type], 'kafkaTopicName', ['kafkaTopicName', 'kafkaTopicCapacity', 'kafkaReplicas', 'serviceName',], type); break
          }
        })
        this.resourceList = JSON.parse(JSON.stringify(this.resourceListINIT))
      } else {
        this.form.getFieldsValue(["username", "phone", "password", "email"]);
      }
    },

  },
  mounted () {
    this.echoUSer();
  },
  created () {
  }
};
</script>
<style lang="less" scoped>
.module {}

.module .titleTop {
  font-weight: 900;
  width: 100%;
  background: #faf8f8;
  border-bottom: 1px solid #ccc;
}

.titleTop span {
  display: inline-block;
  width: 20%;
  text-align: center;
}

.module .moduleItems {
  display: flex;
  border: 1px solid #ccc;
  border-top: none;
}

.module .moduleItems .name {
  flex: .18;
  font-weight: 900;
  text-align: center;
  border-right: 1px solid #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.module .moduleItems .right {
  flex: .82;
}

.module .moduleItems .right .title {
  display: flex;
  justify-content: flex-start;
  border-bottom: 1px solid #ccc;
}

.module .moduleItems .right .title span {
  display: inline-block;
  flex: .41;
  text-indent: 20px;
}

.module .moduleItems .right .title .handle {
  display: inline-block;
  flex: .1;
}

.module .moduleItems .right .content {
  display: flex;
  justify-content: flex-start;
  border-bottom: 1px solid #ccc;
  align-items: center;
}

.module .moduleItems .right .content .btn-opt {
  width: 5%;
}

.module .moduleItems .clearDom {
  border-left: 1px solid #ccc;
  display: flex;
  align-items: center;
}

.module .moduleItems .clearDom .clearIcon {
  color: #db0315;
  font-size: 18px;
  vertical-align: middle;
  text-align: center;
  margin: 8px;

  :hover {
    color: @text-color;
  }
}

.plusIcon {
  color: #1979b9;
  font-size: 16px;
  vertical-align: middle;
  text-align: center;
  margin-left: 9px;

  :hover {
    color: @text-color;
  }
}
</style>
