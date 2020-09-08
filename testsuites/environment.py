#!/usr/bin/env python3

import datetime
import logging

from dit.commands.command_env import CommandEnv
from dit.commands.context import DitContext

logger = logging.getLogger("dit.environment")


def before_all(context):
    settings = CommandEnv.load_settings()
    context.config.setup_logging(filename=CommandEnv.setup_logging(context))
    context.is_recording = False
    DitContext.set_context_dbcjsshell(context, settings)


def before_feature(context, feature):
    logging.info("Begin feature %s: %s", feature.filename, feature.name)
    filename = feature.filename
    filename = filename[:filename.rfind('/')]
    filename = filename[filename.rfind('/') + 1:]
    context.feature_start = datetime.datetime.now()
    context.tracking_id = "%s_%s" % (filename,
                                     datetime.datetime.strftime(context.feature_start, '%Y-%m-%dT%H:%M:%S'))
    logging.info("Begin feature. trackingId '%s'. File %s: %s", context.tracking_id, feature.filename, feature.name, )


def get_groups(feature):
    test_dir = CommandEnv.HOME_DIR + "/" + feature.filename.replace("/test.feature", "")
    return CommandEnv.get_feature_group(test_dir)


def before_scenario(context, feature):
    pass


def after_feature(context, feature):
    duration = datetime.datetime.now() - context.feature_start
    logging.info("End feature. trackingId '%s'. Duration %s seconds. File %s: %s", context.tracking_id,
                 duration.total_seconds(), feature.filename, feature.name)


def after_scenario(context, scenario):
    pass


def after_all(context):
    pass
