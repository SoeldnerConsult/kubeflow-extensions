
from kale.common import kfputils;

_err_counter=0
def _wait_kfp_run_patched(run_id: str, namespace: str = 'kubeflow'):

    global _err_counter
    max_tries = 3

    logger = kfputils._get_logger()

    logger.info('Watching for Run with ID: %s in namespace %s', run_id, namespace)

    while True:
        kfputils.time.sleep(30)

        try:
            run = kfputils.get_run(run_id, namespace=namespace)
            status = run.run.status
            logger.info('Run status: %s', status)

            #reset counter, because call was okay
            _err_counter = 0

            if status not in kfputils.KFP_RUN_FINAL_STATES:
                continue

            return status

        except BaseException as e:
            _err_counter += 1
            logger.info('failed retrieving status, retrying %s times', max_tries - _err_counter)

            if _err_counter > max_tries :
                logger.info('call failed multiple times in a row.. skipping for now')
                raise e

            continue

kfputils._wait_kfp_run = _wait_kfp_run_patched;