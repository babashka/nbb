import { loadFile } from 'nbb';
import { environment, Detail } from '@raycast/api';
import  { useAsync } from "react-async";
import 'nbb/lib/nbb_reagent.js';

const loadScript = async ({}) => {
  const res = await loadFile(environment.assetsPath+'/script.cljs')
  return res;
}

export default function Command () {
  const { data, error, isPending } = useAsync({ promiseFn: loadScript });
  if (data) {
    return <data.Command/>;
  }
  else {
    const txt = (error ? error.toString() : "loading...");
    return <Detail markdown={txt} />
  }
}
